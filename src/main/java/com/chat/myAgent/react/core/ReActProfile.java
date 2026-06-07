package com.chat.myAgent.react.core;

import com.chat.myAgent.react.model.ReActTraceDocument;

import java.util.List;
import java.util.Map;

public interface ReActProfile {

    String domain();

    String agentType();

    String defaultStrategy();

    String initialThought(ReActRunRequest request);

    List<ReActTool> selectTools(ReActRunRequest request);

    String buildAnswerPrompt(ReActRunRequest request,
                             String context,
                             List<String> observations,
                             List<ReActTraceDocument.ReActSourceRefDocument> sources);

    String localAnswer(ReActRunRequest request, String context, List<String> observations);

    List<Map<String, Object>> suggestedActions(ReActRunRequest request,
                                               ReActTraceDocument trace,
                                               List<ReActTraceDocument.ReActSourceRefDocument> sources);

    default String resolveStrategy(ReActRunRequest request) {
        if (request.getStrategy() != null && !request.getStrategy().isBlank()) {
            return request.getStrategy();
        }
        return defaultStrategy();
    }

    default void afterCompleted(ReActRunRequest request, ReActTraceDocument trace) {
    }
}
