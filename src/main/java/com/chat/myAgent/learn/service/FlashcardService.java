package com.chat.myAgent.learn.service;

import com.chat.myAgent.learn.dto.FlashcardGenerateRequest;
import com.chat.myAgent.learn.dto.FlashcardReviewRequest;
import com.chat.myAgent.learn.model.FlashcardDocument;
import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import com.chat.myAgent.learn.repository.mongo.FlashcardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final NoteService noteService;
    // 复习结果会双向驱动笔记掌握度,形成"学习闭环"
    private final StudyService studyService;

    /**
     * 为指定笔记生成闪卡(V1:用模板生成,不做 LLM 调用以节省成本)
     * 1. 第 1 张:核心主题
     * 2. 第 2 张:摘要概括
     * 3. 第 3..N 张:按标签循环
     */
    public Map<String, Object> generate(String userId, FlashcardGenerateRequest request) {
        // 校验笔记属于当前用户(权限控制)
        KnowledgeNoteDocument note = noteService.getOwned(userId, request.getNoteId());
        // count 限制在 1-20,防止用户请求生成 10000 张闪卡
        int count = Math.max(1, Math.min(20, request.getCount() == null ? 10 : request.getCount()));
        List<FlashcardDocument> cards = new ArrayList<>();
        // 固定生成 2 张"基础卡"(主题 + 摘要)
        cards.add(saveCard(userId, note, "这条笔记的核心主题是什么？", note.getTitle()));
        cards.add(saveCard(userId, note, "请概括《" + note.getTitle() + "》", note.getSummary()));
        // 剩余 count-2 张按标签循环
        for (int i = 2; i < count; i++) {
            String tag = note.getTags() != null && !note.getTags().isEmpty()
                    ? note.getTags().get((i - 2) % note.getTags().size())
                    : note.getTitle();
            cards.add(saveCard(userId, note, "与该笔记相关的关键词是什么？", tag));
        }
        // 记录"生成"活动
        studyService.record(userId, "flashcard_generate", note.getTitle(), note.getTags(), note.getCategory(),
                null, null, note.getNoteId(), null, null, 0, null, 0,
                Map.of("generated", cards.size()));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", cards);
        return response;
    }

    /**
     * 查询今日待复习的闪卡(按 nextReviewAt 升序)
     */
    public List<FlashcardDocument> due(String userId) {
        return flashcardRepository.findByUserIdAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(userId, LocalDateTime.now());
    }

    /**
     * 列出用户所有闪卡(按更新时间倒序)
     */
    public List<FlashcardDocument> list(String userId) {
        return flashcardRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * SM-2 算法的复习入口
     *
     * SM-2 三个核心变量:
     *   - intervalDays: 下次复习间隔天数(初始 1)
     *   - easeFactor:   难度系数(初始 2.5,范围 ≥ 1.3)
     *   - reviewCount:  连续正确次数(初始 0)
     *
     * 评分(quality): 0-5
     *   5: 完美,无需思考
     *   4: 犹豫后想起来了
     *   3: 勉强答对
     *   2: 答错但看起来熟悉
     *   1: 答错且不熟悉
     *   0: 完全忘记
     */
    public Map<String, Object> review(String userId, String cardId, FlashcardReviewRequest request) {
        // 1. 取出闪卡,加载当前 SM-2 状态
        FlashcardDocument card = flashcardRepository.findByCardIdAndUserId(cardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("闪卡不存在或无权访问"));
        int quality = request.getQuality() == null ? 0 : request.getQuality();
        // 兼容历史数据:字段可能为 null,用默认值兜底
        int previousInterval = card.getIntervalDays() == null ? 1 : card.getIntervalDays();
        double easeFactor = card.getEaseFactor() == null ? 2.5 : card.getEaseFactor();
        int reviewCount = card.getReviewCount() == null ? 0 : card.getReviewCount();
        int lapseCount = card.getLapseCount() == null ? 0 : card.getLapseCount();
        int nextInterval;

        // ========== 2. SM-2 核心计算 ==========
        if (quality >= 3) {
            // 记住了:interval 按规则增长,easeFactor 微调
            reviewCount += 1;
            if (reviewCount == 1) {
                // 首次答对:明天再复习
                nextInterval = 1;
            } else if (reviewCount == 2) {
                // 第二次答对:6 天后复习
                nextInterval = 6;
            } else {
                // 第 3+ 次答对:按 easeFactor 指数增长
                // 例: interval=6, easeFactor=2.5 → 下次 15 天后再复习
                nextInterval = Math.max(1, (int) Math.round(previousInterval * easeFactor));
            }
            // easeFactor 调节公式: easeFactor + 0.1 - (5-quality)*(0.08 + (5-quality)*0.02)
            //   quality=5: +0.1(变容易)
            //   quality=4:  0  (不变)
            //   quality=3: -0.14(变难)
            // 最低 1.3:防止 easeFactor 过小导致 interval 增长停滞
            easeFactor = Math.max(1.3, easeFactor + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        } else {
            // 忘记了:interval 重置为 1,easeFactor 下降
            reviewCount = 0;       // 连续正确次数清零
            lapseCount += 1;       // 遗忘次数 +1(用于统计"难记"卡片)
            nextInterval = 1;      // 间隔重置为 1 天
            // 每次忘记减 0.2,同样最低 1.3
            easeFactor = Math.max(1.3, easeFactor - 0.2);
        }

        // ========== 3. 掌握度联动:quality → masteryDelta ==========
        // 注意:masteryDelta 范围 [-6, +5],小幅波动不会让掌握度剧变
        int masteryDelta = switch (quality) {
            case 5 -> 5;   // 完美 +5
            case 4 -> 3;   // 犹豫 +3
            case 3 -> 1;   // 勉强 +1
            case 2 -> -2;  // 错 -2
            case 1 -> -4;  // 大错 -4
            default -> -6; // 完全忘记 -6
        };

        // ========== 4. 持久化闪卡新状态 ==========
        card.setReviewCount(reviewCount);
        card.setLapseCount(lapseCount);
        card.setEaseFactor(easeFactor);
        card.setIntervalDays(nextInterval);
        card.setLastReviewAt(LocalDateTime.now());
        card.setNextReviewAt(LocalDateTime.now().plusDays(nextInterval));
        card.setUpdatedAt(LocalDateTime.now());
        flashcardRepository.save(card);

        // ========== 5. 双向联动:更新笔记掌握度 ==========
        // 这就是"学习闭环":闪卡复习 → 笔记掌握度变化 → 薄弱点识别
        studyService.applyMasteryDelta(userId, card.getNoteId(), masteryDelta, "闪卡复习评分 " + quality);

        // ========== 6. 记录学习活动(供学习数据闭环统计) ==========
        studyService.record(userId, "flashcard_review", card.getFront(), card.getTags(), card.getCategory(),
                null, null, card.getNoteId(), null, card.getCardId(), 0, quality * 20, masteryDelta,
                Map.of("quality", quality, "previousIntervalDays", previousInterval, "nextIntervalDays", nextInterval));

        // ========== 7. 返回复习结果(前端展示) ==========
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("cardId", card.getCardId());
        response.put("quality", quality);
        response.put("remembered", quality >= 3);
        response.put("previousIntervalDays", previousInterval);
        response.put("nextIntervalDays", nextInterval);
        response.put("nextReviewAt", card.getNextReviewAt());
        response.put("easeFactor", card.getEaseFactor());
        response.put("masteryDelta", masteryDelta);
        return response;
    }

    /**
     * 保存单张闪卡到 MongoDB
     * 初始化 SM-2 默认值:easeFactor=2.5, intervalDays=1, nextReviewAt=明天
     */
    private FlashcardDocument saveCard(String userId, KnowledgeNoteDocument note, String front, String back) {
        LocalDateTime now = LocalDateTime.now();
        return flashcardRepository.save(FlashcardDocument.builder()
                .cardId("card-" + UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .noteId(note.getNoteId())
                .front(front)
                // back 为空时用笔记摘要兜底
                .back(back == null || back.isBlank() ? note.getSummary() : back)
                .tags(note.getTags())
                .category(note.getCategory())
                // SM-2 初始值:easeFactor=2.5, intervalDays=1
                .easeFactor(2.5)
                .intervalDays(1)
                .reviewCount(0)
                .lapseCount(0)
                .nextReviewAt(now.plusDays(1))
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
