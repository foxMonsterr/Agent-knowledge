package com.chat.myAgent.agent;

import com.chat.myAgent.agent.react.GeneralReActProfile;
import com.chat.myAgent.model.dto.AgentRequest;
import com.chat.myAgent.model.vo.AgentResponse;
import com.chat.myAgent.react.core.ReActEngine;
import com.chat.myAgent.react.core.ReActRunRequest;
import com.chat.myAgent.react.core.ReActRunResult;
import com.chat.myAgent.react.model.ReActTraceDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GeneralReActAgent {

    private final ReActEngine reActEngine;
    private final GeneralReActProfile generalReActProfile;

    public AgentResponse chat(String userId, AgentRequest request, String mode) {
        ReActRunResult result = reActEngine.chat(generalReActProfile, toRunRequest(userId, request, mode));
        ReActTraceDocument trace = result.getTrace();
        return AgentResponse.builder()
                .conversationId(trace.getSessionId())
                .reply(trace.getFinalAnswer())
                .model(trace.getModel())
                .agentType(trace.getAgentType())
                .thinking(toThinking(trace))
                .traceId(trace.getTraceId())
                .build();
    }

    public Flux<String> stream(String userId, AgentRequest request, String mode) {
        return reActEngine.stream(generalReActProfile, toRunRequest(userId, request, mode));
    }

    public ReActTraceDocument getTrace(String userId, String traceId) {
        return reActEngine.getTrace(userId, traceId, generalReActProfile.domain());
    }

    public List<ReActTraceDocument> listTraces(String userId, String sessionId) {
        return reActEngine.listTraces(userId, generalReActProfile.domain(), sessionId);
    }

    public Map<String, Object> intervene(String userId, String traceId, Integer stepNumber, String message) {
        return reActEngine.intervene(userId, traceId, generalReActProfile.domain(), stepNumber, message);
    }

    public Map<String, Object> stop(String userId, String traceId) {
        return reActEngine.stop(userId, traceId, generalReActProfile.domain());
    }

    private ReActRunRequest toRunRequest(String userId, AgentRequest request, String mode) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "agent-" + UUID.randomUUID().toString().replace("-", "");
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mode", mode == null ? "chat" : mode);
        metadata.put("thinkingMode", true);
        return ReActRunRequest.builder()
                .userId(userId)
                .sessionId(conversationId)
                .message(request.getMessage())
                .strategy("auto")
                .memoryEnabled(request.getMemoryEnabled())
                .toolNames(request.getTools() == null ? List.of() : request.getTools())
                .metadata(metadata)
                .build();
    }

    private String toThinking(ReActTraceDocument trace) {
        return trace.getSteps().stream()
                .map(step -> {
                    if (step.getThought() != null) {
                        return "Thought: " + step.getThought();
                    }
                    if (step.getActionName() != null) {
                        return "Action: " + step.getActionName();
                    }
                    if (step.getObservation() != null) {
                        return "Observation: " + step.getObservation();
                    }
                    return step.getType();
                })
                .collect(Collectors.joining("\n"));
    }
}
