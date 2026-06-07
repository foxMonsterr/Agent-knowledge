package com.chat.myAgent.learn.service;

import com.chat.myAgent.learn.dto.FeynmanEvaluateRequest;
import com.chat.myAgent.learn.dto.QuizEvaluateRequest;
import com.chat.myAgent.learn.dto.QuizGenerateRequest;
import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import com.chat.myAgent.learn.model.QuizDocument;
import com.chat.myAgent.learn.repository.mongo.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final NoteService noteService;
    // 答题/费曼结果会驱动笔记掌握度,形成学习闭环
    private final StudyService studyService;
    // 答题正确时发 "StageCompletedEvent",供 LearningPathService 监听并更新学习路径进度
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 为指定笔记生成一组测验题(V1:模板生成,不做 LLM 调用以节省成本)
     *
     * 3 种题型按 index%3 轮换:
     *   index%3==0: 单选题(默认第一道)
     *   index%3==1: 判断题
     *   index%3==2: 简答题
     *
     * 同一组题的 quizSetId 相同,前端可以用一个 quizSetId 拉一整组题
     */
    public Map<String, Object> generate(String userId, QuizGenerateRequest request) {
        // 优先按 noteId 找笔记;没有 noteId 时按 topic 模糊匹配第一篇
        KnowledgeNoteDocument note = resolveNote(userId, request.getNoteId(), request.getTopic());
        // count 限制在 1-20,防止用户请求 10000 道题
        int count = Math.max(1, Math.min(20, request.getCount() == null ? 5 : request.getCount()));
        // quizSetId 用于把同一组生成的题关联起来(便于前端一次性展示)
        String quizSetId = "quizset-" + UUID.randomUUID().toString().replace("-", "");
        List<QuizDocument> saved = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            saved.add(quizRepository.save(buildQuiz(userId, note, quizSetId, request.getDifficulty(), i)));
        }
        // 记录"生成"活动
        studyService.record(userId, "quiz_generate", note.getTitle(), note.getTags(), note.getCategory(),
                null, null, note.getNoteId(), null, null, 0, null, 0,
                Map.of("quizSetId", quizSetId, "count", saved.size()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("quizSetId", quizSetId);
        response.put("items", saved);
        return response;
    }

    /**
     * 评估用户作答
     *
     * 评分逻辑:
     *   - 答案归一化(去空格 + 转小写)后,要么完全相等,要么用户答案包含标准答案
     *   - 答对: score=100, masteryDelta 取 difficulty 映射值
     *   - 答错: score=40,  掌握度减半(用 abs(delta)/2 兜底 2)
     *
     * 注意: 答对时会发 LearningPathService.StageCompletedEvent,
     *       供学习路径服务更新阶段进度(事件驱动,解耦)
     */
    public Map<String, Object> evaluate(String userId, QuizEvaluateRequest request) {
        QuizDocument quiz = quizRepository.findByQuizIdAndUserId(request.getQuizId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("测验不存在或无权访问"));
        // 答案归一化:trim + toLowerCase,避免大小写/空格导致的"误判错"
        // 简答题用了"包含"判断: 用户答案包含标准答案关键词就算对(降低判分严格度)
        boolean correct = normalize(quiz.getCorrectAnswer()).equals(normalize(request.getUserAnswer()))
                || normalize(request.getUserAnswer()).contains(normalize(quiz.getCorrectAnswer()));
        int score = correct ? 100 : 40;
        // 难度越高的题,掌握度奖励越大(easy=+2, medium=+4, hard=+6)
        int delta = correct ? masteryDelta(quiz.getDifficulty()) : -Math.max(2, masteryDelta(quiz.getDifficulty()) / 2);
        // 掌握度联动
        studyService.applyMasteryDelta(userId, quiz.getNoteId(), delta, correct ? "测验答对" : "测验答错");
        // 记录答题活动
        studyService.record(userId, "quiz_answer", quiz.getQuestion(), quiz.getTags(), quiz.getCategory(),
                null, null, quiz.getNoteId(), quiz.getQuizId(), null, 0, score, delta,
                Map.of("correct", correct, "difficulty", quiz.getDifficulty(), "userAnswer", request.getUserAnswer()));

        // 构造响应
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("quizId", quiz.getQuizId());
        response.put("correct", correct);
        response.put("score", score);
        response.put("feedback", correct ? "回答正确。" : "回答不完整，建议回看关联笔记。");
        response.put("explanation", quiz.getExplanation());
        response.put("missedPoints", correct ? List.of() : List.of("请重点复习：" + quiz.getQuestion()));
        response.put("masteryDelta", delta);

        // 答对且有关联笔记时,发阶段完成事件(LearningPathService 会监听)
        if (correct && quiz.getNoteId() != null) {
            eventPublisher.publishEvent(new LearningPathService.StageCompletedEvent(userId, quiz.getNoteId(), score));
        }
        return response;
    }

    /**
     * 费曼检验评估
     *
     * 费曼技巧:让用户用自己的话解释一个概念,看是否真正理解
     * V1 版本用启发式评分(没调 LLM):
     *   - coverage(覆盖度): 用户解释中是否包含笔记的标签关键词
     *   - accuracy(准确度): 解释长度 > 80 字符视为"说得够多"
     *   - clarity(清晰度):  解释长度 > 40 字符视为"表达清晰"
     *   - 最终分 = 三者平均,映射到掌握度增量
     *
     * TODO: V2 接入 LLM 做语义级评估
     */
    public Map<String, Object> evaluateFeynman(String userId, FeynmanEvaluateRequest request) {
        // 校验笔记属于当前用户
        KnowledgeNoteDocument note = noteService.getOwned(userId, request.getNoteId());
        String explanation = request.getExplanation();
        // 覆盖度: 解释中是否包含至少一个笔记标签 → 78(高) / 58(低)
        int coverage = containsAny(explanation, note.getTags()) ? 78 : 58;
        // 准确度: 解释够长视为"答得详细" → 82 / 65
        int accuracy = explanation.length() > 80 ? 82 : 65;
        // 清晰度: 解释超过 40 字符视为"表达清晰" → 80 / 60
        int clarity = explanation.length() > 40 ? 80 : 60;
        // 三维度平均(四舍五入)
        int score = Math.round((coverage + accuracy + clarity) / 3.0f);
        // 掌握度映射: 优秀 +8 / 良好 +5 / 合格 +2 / 差 -5
        int delta = score >= 90 ? 8 : score >= 75 ? 5 : score >= 60 ? 2 : -5;
        studyService.applyMasteryDelta(userId, note.getNoteId(), delta, "费曼检验");
        studyService.record(userId, "feynman_evaluate", note.getTitle(), note.getTags(), note.getCategory(),
                null, null, note.getNoteId(), null, null, 0, score, delta,
                Map.of("coverage", coverage / 100.0, "accuracy", accuracy / 100.0, "clarity", clarity / 100.0));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("evaluationId", "feynman-" + UUID.randomUUID().toString().replace("-", ""));
        response.put("noteId", note.getNoteId());
        // 三维度都除以 100 转成 0-1 范围(前端可直接拿去做图表)
        response.put("coverage", coverage / 100.0);
        response.put("accuracy", accuracy / 100.0);
        response.put("clarity", clarity / 100.0);
        response.put("score", score);
        response.put("missedPoints", score >= 75 ? List.of() : List.of("解释中缺少与原笔记标签相关的关键概念"));
        response.put("misconceptions", List.of());
        response.put("feedback", score >= 75 ? "解释整体清晰，可以进入练习巩固。" : "解释偏简略，建议先回看笔记再用自己的话复述。");
        // 建议掌握度 = 分数 clamp 到 [0, 100](这里其实已经是,作为防御)
        response.put("suggestedMastery", Math.max(0, Math.min(100, score)));
        return response;
    }

    /**
     * 列出用户的测验
     * - noteId 不为空: 列出该笔记的测验
     * - noteId 为空: 列出用户所有测验
     */
    public List<QuizDocument> list(String userId, String noteId) {
        if (noteId != null && !noteId.isBlank()) {
            return quizRepository.findByUserIdAndNoteIdOrderByCreatedAtDesc(userId, noteId);
        }
        return quizRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 解析"要测哪条笔记"
     * 优先级: noteId > topic
     * - 有 noteId: 直接按 ID 取(精确)
     * - 没 noteId 但有 topic: 在用户笔记列表里按标题模糊匹配第一篇
     * - 都没有: 抛异常引导用户先创建笔记
     */
    private KnowledgeNoteDocument resolveNote(String userId, String noteId, String topic) {
        if (noteId != null && !noteId.isBlank()) {
            return noteService.getOwned(userId, noteId);
        }
        return noteService.list(userId, false).stream()
                .filter(note -> topic == null || topic.isBlank() || note.getTitle().contains(topic))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("请先创建或选择一条笔记"));
    }

    /**
     * 构造一道题(V1 用模板,不做 LLM 生成)
     * 3 种题型按 index%3 轮换,保证一组题有变化
     */
    private QuizDocument buildQuiz(String userId, KnowledgeNoteDocument note, String quizSetId, String difficulty, int index) {
        String quizId = "quiz-" + UUID.randomUUID().toString().replace("-", "");
        // 判断题: index=1, 4, 7 ...
        if (index % 3 == 1) {
            return QuizDocument.builder()
                    .quizId(quizId)
                    .quizSetId(quizSetId)
                    .userId(userId)
                    .noteId(note.getNoteId())
                    .type("true_false")
                    // 万能判断题,正确率几乎 100%,用作"开场热身"
                    .question("判断题：\"" + note.getTitle() + "\" 是你知识库中的一个学习主题。")
                    .correctAnswer("true")
                    .explanation("该题用于确认你能识别当前复习主题。")
                    .difficulty(normalizeDifficulty(difficulty))
                    .tags(note.getTags())
                    .category(note.getCategory())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
        // 简答题: index=2, 5, 8 ...
        if (index % 3 == 2) {
            return QuizDocument.builder()
                    .quizId(quizId)
                    .quizSetId(quizSetId)
                    .userId(userId)
                    .noteId(note.getNoteId())
                    .type("short_answer")
                    .question("请用自己的话解释：" + note.getTitle())
                    // 标准答案直接用笔记摘要
                    .correctAnswer(note.getSummary())
                    .explanation("参考笔记摘要：" + note.getSummary())
                    .difficulty(normalizeDifficulty(difficulty))
                    .tags(note.getTags())
                    .category(note.getCategory())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
        // 单选题: index=0, 3, 6 ... (默认/兜底)
        return QuizDocument.builder()
                .quizId(quizId)
                .quizSetId(quizSetId)
                .userId(userId)
                .noteId(note.getNoteId())
                .type("single_choice")
                .question("以下哪一项最接近笔记《" + note.getTitle() + "》的核心内容？")
                // A 是笔记摘要(正确答案),B/C/D 是明显的干扰项
                .options(List.of(
                        QuizDocument.QuizOption.builder().key("A").text(note.getSummary()).build(),
                        QuizDocument.QuizOption.builder().key("B").text("与当前主题无关的随机描述").build(),
                        QuizDocument.QuizOption.builder().key("C").text("只需要跳过该主题").build(),
                        QuizDocument.QuizOption.builder().key("D").text("该主题没有任何可复习内容").build()
                ))
                .correctAnswer("A")
                .explanation("选项 A 来自笔记摘要，最贴近当前知识点。")
                .difficulty(normalizeDifficulty(difficulty))
                .tags(note.getTags())
                .category(note.getCategory())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 难度归一化: 空值/异常值统一成 "medium",防止下游 switch 抛 NPE
     */
    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return "medium";
        }
        return difficulty.toLowerCase();
    }

    /**
     * 难度 → 掌握度奖励映射
     * 答对时: easy=+2, medium=+4, hard=+6(难题答对奖励更大)
     */
    private int masteryDelta(String difficulty) {
        return switch (normalizeDifficulty(difficulty)) {
            case "easy" -> 2;
            case "hard" -> 6;
            default -> 4;
        };
    }

    /**
     * 答案归一化: 去空格 + 转小写
     * 用于评分时避免"True"和"true"被算成不同答案
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /**
     * 文本是否包含任一关键词(费曼检验的覆盖度判断)
     * - 文本为 null 或 tags 为 null: 返回 false
     * - 至少有一个非空 tag 被文本包含: 返回 true
     */
    private boolean containsAny(String text, List<String> tags) {
        if (text == null || tags == null) {
            return false;
        }
        return tags.stream().anyMatch(tag -> tag != null && !tag.isBlank() && text.contains(tag));
    }
}
