package com.chat.myAgent.react.core;

import com.chat.myAgent.react.model.ReActTraceDocument;
import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class ReActRunResult {
    private ReActTraceDocument trace;

    public Map<String, Object> toResponseMap() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("traceId", trace.getTraceId());
        response.put("sessionId", trace.getSessionId());
        response.put("conversationId", trace.getSessionId());
        response.put("answer", trace.getFinalAnswer());
        response.put("reply", trace.getFinalAnswer());
        response.put("steps", trace.getSteps());
        response.put("sources", trace.getFinalSources());
        response.put("model", trace.getModel());
        response.put("fallbackUsed", trace.getFallbackUsed());
        response.put("agentType", trace.getAgentType());
        response.put("totalDurationMs", trace.getTotalDurationMs());
        return response;
    }
}
