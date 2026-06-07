package com.chat.myAgent.learn.service;

import com.chat.myAgent.learn.model.FlashcardDocument;
import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import com.chat.myAgent.learn.model.StudyRecordEntity;
import com.chat.myAgent.learn.repository.jpa.StudyRecordRepository;
import com.chat.myAgent.learn.repository.mongo.FlashcardRepository;
import com.chat.myAgent.learn.repository.mongo.KnowledgeNoteRepository;
import com.chat.myAgent.learn.repository.mongo.QuizRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRecordRepository studyRecordRepository;
    private final KnowledgeNoteRepository noteRepository;
    private final QuizRepository quizRepository;
    private final FlashcardRepository flashcardRepository;
    // 用于把 detail Map 序列化为 JSON 字符串存到 MySQL
    private final ObjectMapper objectMapper;

    /**
     * 学习活动统一记录入口
     *
     * 7 种活动类型(通过 activityType 区分):
     *   - note_create / note_update   笔记 CRUD
     *   - document_import             文档导入
     *   - quiz_generate / quiz_answer 测验生成/答题
     *   - feynman_evaluate            费曼检验
     *   - flashcard_review            闪卡复习
     *   - mastery_update              掌握度变更
     *   - chat                        ReAct 对话
     *
     * 所有学习行为都过这里,后续做"学习行为分析"时只需 SQL 聚合 study_record 表
     */
    public StudyRecordEntity record(String userId, String activityType, String topic, List<String> tags,
                                    String category, String sessionId, String traceId, String noteId,
                                    String quizId, String cardId, Integer durationSeconds, Integer score,
                                    Integer masteryDelta, Map<String, Object> detail) {
        StudyRecordEntity entity = StudyRecordEntity.builder()
                .recordId("study-" + UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .activityType(activityType)
                .topic(topic)
                // 标签数组转 CSV 字符串,便于 SQL 模糊查询
                .tags(tags == null ? "" : String.join(",", tags))
                .category(category)
                .sessionId(sessionId)
                .traceId(traceId)
                .noteId(noteId)
                .quizId(quizId)
                .cardId(cardId)
                .durationSeconds(durationSeconds == null ? 0 : durationSeconds)
                .score(score)
                .masteryDelta(masteryDelta)
                // detail 是扩展字段(JSON 形式),每种活动可以放自己的上下文
                .detail(toJson(detail))
                .createdAt(LocalDateTime.now())
                .build();
        return studyRecordRepository.save(entity);
    }

    /**
     * 学习概览(首页/看板数据)
     *
     * 聚合 4 类数据:
     *   1. 今日统计(时长/测验数/复习数/创建笔记数)
     *   2. 总计统计(笔记数/文档数/测验数/闪卡数/已掌握/薄弱)
     *   3. 最近学习主题(top 10 笔记)
     *   4. 薄弱主题(top 10 掌握度 < 40 的笔记)
     */
    public Map<String, Object> overview(String userId) {
        // 今日 0 点(本地时区)作为分界
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        // 查询今日所有学习记录
        List<StudyRecordEntity> todayRecords = studyRecordRepository.findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(userId, todayStart);
        // 查询用户最近的所有学习记录(供"最近活动"列表)
        List<StudyRecordEntity> recentRecords = studyRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        // 查询用户所有未归档笔记
        List<KnowledgeNoteDocument> notes = noteRepository.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, false);
        // 查询今日待复习闪卡
        List<FlashcardDocument> dueCards = flashcardRepository.findByUserIdAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(userId, LocalDateTime.now());

        // ========== 今日数据 ==========
        Map<String, Object> today = new LinkedHashMap<>();
        // 学习总分钟数(秒数求和再除以 60)
        today.put("studiedMinutes", todayRecords.stream().mapToInt(r -> value(r.getDurationSeconds())).sum() / 60);
        today.put("completedQuizCount", countActivity(todayRecords, "quiz_answer"));
        today.put("reviewedCardCount", countActivity(todayRecords, "flashcard_review"));
        today.put("createdNoteCount", countActivity(todayRecords, "note_create"));
        today.put("dueReviewCount", dueCards.size());

        // ========== 总计数据 ==========
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("noteCount", notes.size());
        // 历史导入的文档数(从 study_record 表的 activityType 聚合)
        totals.put("documentCount", studyRecordRepository.countByUserIdAndActivityType(userId, "document_import"));
        totals.put("quizCount", quizRepository.countByUserId(userId));
        totals.put("flashcardCount", flashcardRepository.countByUserId(userId));
        // 掌握度 ≥ 80 视为已掌握
        totals.put("masteredNoteCount", noteRepository.countByUserIdAndMasteryLevelGreaterThanEqualAndArchived(userId, 80, false));
        // 掌握度 < 40 视为薄弱
        totals.put("weakNoteCount", noteRepository.countByUserIdAndMasteryLevelLessThanAndArchived(userId, 40, false));

        // ========== 组装结果 ==========
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("today", today);
        result.put("totals", totals);
        result.put("recentTopics", buildTopics(notes));
        result.put("weakTopics", buildWeakness(userId, 10));
        result.put("dueCards", dueCards);
        result.put("recentActivities", recentRecords.stream().limit(20).toList());
        return result;
    }

    /**
     * 薄弱点查询(给前端干预建议用)
     */
    public List<Map<String, Object>> weakness(String userId, int limit) {
        return buildWeakness(userId, limit);
    }

    /**
     * 应用掌握度变化(闪卡复习、测验答题都走这里)
     *
     * 这是"学习闭环"的核心:任何学习行为都会触发掌握度变化
     * clamp(0, 100) 保证掌握度始终在 [0, 100] 范围内
     */
    public void applyMasteryDelta(String userId, String noteId, int delta, String reason) {
        if (noteId == null || noteId.isBlank()) {
            return;
        }
        // 找到笔记(若不存在则静默返回,不抛异常)
        noteRepository.findByNoteIdAndUserId(noteId, userId).ifPresent(note -> {
            int current = note.getMasteryLevel() == null ? 0 : note.getMasteryLevel();
            // 限制在 [0, 100],防止掌握度溢出
            note.setMasteryLevel(Math.max(0, Math.min(100, current + delta)));
            note.setUpdatedAt(LocalDateTime.now());
            noteRepository.save(note);
            // 记录"掌握度变更"事件,detail 含原因和前后值(供审计/回滚)
            record(userId, "mastery_update", note.getTitle(), note.getTags(), note.getCategory(),
                    null, null, noteId, null, null, 0, null, delta,
                    Map.of("reason", reason, "previousMastery", current, "currentMastery", note.getMasteryLevel()));
        });
    }

    /**
     * 构造"最近主题"列表(top 10)
     * 把笔记转成前端易消费的格式
     */
    private List<Map<String, Object>> buildTopics(List<KnowledgeNoteDocument> notes) {
        return notes.stream()
                .limit(10)
                .map(note -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("topic", note.getTitle());
                    item.put("category", note.getCategory());
                    item.put("tags", note.getTags());
                    item.put("noteCount", 1);
                    item.put("averageMastery", value(note.getMasteryLevel()));
                    item.put("lastStudiedAt", note.getUpdatedAt());
                    return item;
                })
                .toList();
    }

    /**
     * 构造"薄弱点"列表
     * 1. 过滤出掌握度 < 40 的笔记
     * 2. 按掌握度升序(最薄的排最前)
     * 3. 附上"建议下一步动作"(前端可直接渲染成按钮)
     */
    private List<Map<String, Object>> buildWeakness(String userId, int limit) {
        return noteRepository.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, false).stream()
                .filter(note -> value(note.getMasteryLevel()) < 40)
                .sorted(Comparator.comparing(note -> value(note.getMasteryLevel())))
                .limit(Math.max(1, limit))
                .map(note -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("weaknessId", "weak-" + note.getNoteId());
                    item.put("topic", note.getTitle());
                    item.put("noteIds", List.of(note.getNoteId()));
                    item.put("tags", note.getTags());
                    item.put("category", note.getCategory());
                    // 严重度: < 20 高, < 40 中
                    item.put("severity", value(note.getMasteryLevel()) < 20 ? "high" : "medium");
                    item.put("averageMastery", value(note.getMasteryLevel()));
                    item.put("weakReason", "掌握度低于 40，建议重新学习并生成测验。");
                    // 前端可点击的"干预动作",直接跳到对应页面
                    item.put("suggestedActions", List.of(
                            Map.of("action", "start_react_chat", "label", "让 Agent 重新解释", "params", Map.of("noteId", note.getNoteId())),
                            Map.of("action", "generate_quiz", "label", "生成练习题", "params", Map.of("noteId", note.getNoteId()))
                    ));
                    return item;
                })
                .toList();
    }

    /**
     * 雷达图数据:按 category 分组,统计每组的平均掌握度和参与度
     * (供前端 D3.js / ECharts 渲染)
     */
    public Map<String, Object> radar(String userId) {
        List<KnowledgeNoteDocument> notes = noteRepository.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, false);
        // 按 category 分组
        Map<String, List<KnowledgeNoteDocument>> byCategory = new LinkedHashMap<>();
        for (KnowledgeNoteDocument note : notes) {
            String cat = note.getCategory() == null || note.getCategory().isBlank() ? "未分类" : note.getCategory();
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(note);
        }
        // 计算每组的平均掌握度 + 平均参与度
        List<Map<String, Object>> dimensions = new ArrayList<>();
        for (Map.Entry<String, List<KnowledgeNoteDocument>> entry : byCategory.entrySet()) {
            String cat = entry.getKey();
            List<KnowledgeNoteDocument> catNotes = entry.getValue();
            // 平均掌握度
            double avgMastery = catNotes.stream().mapToInt(n -> value(n.getMasteryLevel())).average().orElse(0);
            // 参与度 = 总复习次数 / 笔记数(平均每篇被复习几次)
            double engagement = catNotes.stream().mapToInt(n -> value(n.getReviewCount())).sum() / (double) Math.max(1, catNotes.size());
            Map<String, Object> dim = new LinkedHashMap<>();
            dim.put("dimension", cat);
            dim.put("noteCount", catNotes.size());
            dim.put("averageMastery", Math.round(avgMastery * 100.0) / 100.0);
            dim.put("engagement", Math.round(engagement * 100.0) / 100.0);
            // 维度内的薄弱/已掌握数(便于在该维度上做下钻)
            dim.put("weakNoteCount", catNotes.stream().filter(n -> value(n.getMasteryLevel()) < 40).count());
            dim.put("masteredNoteCount", catNotes.stream().filter(n -> value(n.getMasteryLevel()) >= 80).count());
            dimensions.add(dim);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("totalNotes", notes.size());
        result.put("dimensions", dimensions);
        // 整体平均掌握度
        result.put("overallMastery", notes.isEmpty() ? 0 : Math.round(notes.stream().mapToInt(n -> value(n.getMasteryLevel())).average().orElse(0) * 100.0) / 100.0);
        return result;
    }

    /**
     * 干预建议(薄弱点的简化版,只给动作建议)
     * 给前端"今日推荐"卡片用
     */
    public Map<String, Object> intervention(String userId) {
        // 取 top 20 薄弱点
        List<Map<String, Object>> weakList = buildWeakness(userId, 20);
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Map<String, Object> weak : weakList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("weaknessId", weak.get("weaknessId"));
            item.put("topic", weak.get("topic"));
            item.put("severity", weak.get("severity"));
            item.put("actions", weak.get("suggestedActions"));
            item.put("reason", weak.get("weakReason"));
            suggestions.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("weakCount", suggestions.size());
        result.put("suggestions", suggestions);
        return result;
    }

    /**
     * 统计指定活动类型在 records 中出现次数
     */
    private long countActivity(List<StudyRecordEntity> records, String activityType) {
        return records.stream().filter(record -> activityType.equals(record.getActivityType())).count();
    }

    /**
     * 个性化推荐(基于历史学习数据)
     * 1. 最佳学习时段:按 HOUR(createdAt) 聚合
     * 2. 偏好活动类型:出现频率最高的 activityType
     * 3. 投入最多时间的主题:top 5 by totalSeconds
     * 4. 推荐的薄弱笔记:掌握度 < 40 的 top 5
     *
     * 冷启动:无历史时返回 insufficientData=true
     */
    public Map<String, Object> recommendations(String userId) {
        List<StudyRecordEntity> records = studyRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        if (records.isEmpty()) {
            // 冷启动:没有任何学习记录
            result.put("insufficientData", true);
            result.put("bestStudyHour", null);
            result.put("preferredMode", null);
            result.put("recommendedTopics", List.of());
            result.put("message", "暂无学习记录，开始学习后将提供个性化推荐。");
            return result;
        }
        result.put("insufficientData", false);

        // 1. 最佳学习时段(从 StudyRecordRepository 的 JPQL 聚合查询)
        List<Object[]> hourRows = studyRecordRepository.bestStudyHours(userId);
        result.put("bestStudyHour", hourRows.isEmpty() ? null : Map.of("hour", hourRows.get(0)[0], "sessionCount", hourRows.get(0)[1]));

        // 2. 偏好活动类型(出现频率最高的)
        List<Object[]> modeRows = studyRecordRepository.activityTypeDistribution(userId);
        result.put("preferredMode", modeRows.isEmpty() ? null : Map.of("activityType", modeRows.get(0)[0], "count", modeRows.get(0)[1]));

        // 3. 投入最多时间的主题(top 5)
        List<Object[]> topicRows = studyRecordRepository.topicStudyTime(userId);
        List<Map<String, Object>> topics = topicRows.stream().limit(5).map(row -> {
            Map<String, Object> topic = new LinkedHashMap<>();
            topic.put("topic", row[0]);
            topic.put("totalSeconds", row[1]);
            return topic;
        }).toList();
        result.put("recommendedTopics", topics);

        // 4. 推荐的薄弱笔记(top 5)
        List<KnowledgeNoteDocument> weakNotes = noteRepository.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, false).stream()
                .filter(note -> value(note.getMasteryLevel()) < 40)
                .limit(5)
                .toList();
        List<Map<String, Object>> suggestedNotes = new ArrayList<>();
        for (KnowledgeNoteDocument note : weakNotes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("noteId", note.getNoteId());
            item.put("title", note.getTitle());
            item.put("masteryLevel", value(note.getMasteryLevel()));
            item.put("category", note.getCategory());
            suggestedNotes.add(item);
        }
        result.put("suggestedNotes", suggestedNotes);
        return result;
    }

    /**
     * Integer 拆箱兜底(避免 NPE)
     */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Map → JSON 字符串(序列化失败时返回 "{}" 而非抛异常)
     */
    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail == null ? Map.of() : detail);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
