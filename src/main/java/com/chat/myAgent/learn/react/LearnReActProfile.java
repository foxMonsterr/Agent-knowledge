package com.chat.myAgent.learn.react;

import com.chat.myAgent.learn.service.StudyService;
import com.chat.myAgent.react.core.ReActProfile;
import com.chat.myAgent.react.core.ReActRunRequest;
import com.chat.myAgent.react.core.ReActTool;
import com.chat.myAgent.react.model.ReActTraceDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LearnReActProfile implements ReActProfile {

    private final LearnReActToolRegistry toolRegistry;
    private final StudyService studyService;

    @Override
    public String domain() {
        return "learn";
    }

    @Override
    public String agentType() {
        return "learn-react";
    }

    @Override
    public String defaultStrategy() {
        return "auto";
    }

    @Override
    public String resolveStrategy(ReActRunRequest request) {
        String strategy = ReActProfile.super.resolveStrategy(request);
        if ("auto".equals(strategy)) {
            String inferred = StrategyInferer.infer(request.getMessage());
            return inferred != null ? inferred : strategy;
        }
        return strategy;
    }

    @Override
    public String initialThought(ReActRunRequest request) {
        String strategy = request.getStrategy();
        if ("quiz".equals(strategy)) {
            return "我会根据你的知识库内容生成测验题目，帮助你检验学习效果。";
        }
        if ("review".equals(strategy)) {
            return "我会分析你的学习进度和薄弱环节，帮你制定复习计划。";
        }
        if ("explore".equals(strategy)) {
            return "我会检索你的个人知识库和导入资料，必要时联网搜索补充最新信息。";
        }
        return "我会先检索你的个人知识库和导入资料，再基于来源生成学习回答。";
    }

    @Override
    public List<ReActTool> selectTools(ReActRunRequest request) {
        String strategy = request.getStrategy();
        return switch (strategy) {
            case "quiz" -> List.of(toolRegistry.getQuizTool());
            case "review" -> List.of(toolRegistry.getSearchNotesTool(), toolRegistry.getLearningProgressTool());
            case "explore" -> List.of(toolRegistry.getSearchNotesTool(), toolRegistry.getSearchWebTool(), toolRegistry.getQuizTool());
            default -> toolRegistry.selectTools(request);
        };
    }

    @Override
    public String buildAnswerPrompt(ReActRunRequest request,
                                    String context,
                                    List<String> observations,
                                    List<ReActTraceDocument.ReActSourceRefDocument> sources) {
        String strategy = request.getStrategy();
        String obsText = observations.isEmpty() ? "无" : String.join("\n", observations);
        String srcText = context == null || context.isBlank() ? "未检索到相关来源。" : context;

        if ("quiz".equals(strategy)) {
            return """
                    你是 LearnAgent，一个专注于测验评估的学习助手。
                    要求：
                    1. 基于检索到的知识库内容，生成与用户问题相关的测验题。
                    2. 题目应有明确的考察点和难度标注。
                    3. 如果来源不足，明确说明并建议用户先补充笔记。
                    4. 不要编造不存在的来源。
                    5. 不要输出隐藏推理过程。

                    用户问题：
                    %s

                    工具观察：
                    %s

                    检索来源：
                    %s
                    """.formatted(request.getMessage(), obsText, srcText);
        }

        if ("review".equals(strategy)) {
            return """
                    你是 LearnAgent，一个专注于知识复习与薄弱点分析的学习助手。
                    要求：
                    1. 基于学习进度和薄弱点数据，帮用户定位需要重点复习的内容。
                    2. 给出具体的复习建议和学习路径。
                    3. 如果数据不足，建议用户先进行测验或创建更多笔记。
                    4. 不要编造不存在的来源。
                    5. 不要输出隐藏推理过程。

                    用户问题：
                    %s

                    工具观察：
                    %s

                    检索来源：
                    %s
                    """.formatted(request.getMessage(), obsText, srcText);
        }

        return """
                你是 LearnAgent，一个个人学习知识库助手。请基于给定来源回答用户问题。
                要求：
                1. 用适合学习者的方式解释。
                2. 如果来源不足，明确说明并给出下一步学习建议。
                3. 不要编造不存在的来源。
                4. 不要输出隐藏推理过程。

                用户问题：
                %s

                工具观察：
                %s

                检索来源：
                %s
                """.formatted(request.getMessage(), obsText, srcText);
    }

    @Override
    public String localAnswer(ReActRunRequest request, String context, List<String> observations) {
        if (context == null || context.isBlank()) {
            return "我没有在当前个人知识库中检索到足够相关的内容。建议先导入资料或创建笔记，然后再围绕这个问题继续学习：" + request.getMessage();
        }
        return "我先检索了你的个人知识库。基于当前来源，可以这样理解：\n\n"
                + abbreviate(context, 800)
                + "\n\n建议下一步生成测验或闪卡，用练习确认是否真正掌握。";
    }

    @Override
    public List<Map<String, Object>> suggestedActions(ReActRunRequest request,
                                                      ReActTraceDocument trace,
                                                      List<ReActTraceDocument.ReActSourceRefDocument> sources) {
        return List.of(
                Map.of("action", "generate_quiz", "label", "生成练习题"),
                Map.of("action", "generate_flashcards", "label", "生成复习闪卡"),
                Map.of("action", "save_note", "label", "保存为笔记")
        );
    }

    @Override
    public void afterCompleted(ReActRunRequest request, ReActTraceDocument trace) {
        studyService.record(request.getUserId(), "chat", request.getMessage(), request.getTags(), request.getCategory(),
                trace.getSessionId(), trace.getTraceId(), null, null, null,
                Math.toIntExact(Math.max(0, trace.getTotalDurationMs() / 1000)),
                null, 0, Map.of("strategy", trace.getStrategy(), "sourceCount", trace.getFinalSources().size()));
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= max ? compact : compact.substring(0, max) + "...";
    }
}
