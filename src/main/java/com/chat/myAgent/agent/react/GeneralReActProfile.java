package com.chat.myAgent.agent.react;

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
public class GeneralReActProfile implements ReActProfile {

    private final GeneralReActToolRegistry toolRegistry;

    @Override
    public String domain() {
        return "general";
    }

    @Override
    public String agentType() {
        return "general-react";
    }

    @Override
    public String defaultStrategy() {
        return "auto";
    }

    @Override
    public String initialThought(ReActRunRequest request) {
        return "我会判断是否需要调用本地工具；如果需要，会先执行工具并基于观察结果回答。";
    }

    @Override
    public List<ReActTool> selectTools(ReActRunRequest request) {
        return toolRegistry.selectTools(request);
    }

    @Override
    public String buildAnswerPrompt(ReActRunRequest request,
                                    String context,
                                    List<String> observations,
                                    List<ReActTraceDocument.ReActSourceRefDocument> sources) {
        return """
                你是 SmartAgent 的通用 ReAct 助手。请基于工具观察结果回答用户问题。
                要求：
                1. 不要输出隐藏推理过程。
                2. 如果工具结果不足，请明确说明限制。
                3. 如果用户询问实时天气、联网搜索或外部 HTTP，必须说明当前未接入，不要编造。

                用户问题：
                %s

                工具观察：
                %s

                可用上下文：
                %s
                """.formatted(request.getMessage(),
                observations.isEmpty() ? "本轮未调用工具。" : String.join("\n\n", observations),
                context == null || context.isBlank() ? "无" : context);
    }

    @Override
    public String localAnswer(ReActRunRequest request, String context, List<String> observations) {
        if (observations != null && !observations.isEmpty()) {
            return "基于本轮工具观察结果：\n\n" + String.join("\n\n", observations);
        }
        if (context != null && !context.isBlank()) {
            return "基于已有上下文：\n\n" + context;
        }
        return "当前问题不需要或无法调用本地工具。我可以直接回答普通问题；如果涉及实时天气、联网搜索或外部 HTTP，本项目当前未接入这些能力。";
    }

    @Override
    public List<Map<String, Object>> suggestedActions(ReActRunRequest request,
                                                      ReActTraceDocument trace,
                                                      List<ReActTraceDocument.ReActSourceRefDocument> sources) {
        return List.of(
                Map.of("action", "retry_without_react", "label", "普通模式重试"),
                Map.of("action", "show_tools", "label", "查看可用工具")
        );
    }
}
