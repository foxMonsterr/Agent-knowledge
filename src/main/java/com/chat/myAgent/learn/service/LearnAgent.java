package com.chat.myAgent.learn.service;

import com.chat.myAgent.learn.dto.ReActChatRequest;
import com.chat.myAgent.learn.react.LearnReActProfile;
import com.chat.myAgent.react.core.ReActEngine;
import com.chat.myAgent.react.core.ReActRunRequest;
import com.chat.myAgent.react.model.ReActTraceDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LearnAgent {

    private final ReActEngine reActEngine;
    private final LearnReActProfile learnReActProfile;

    public Map<String, Object> chat(String userId, ReActChatRequest request) {
        return reActEngine.chat(learnReActProfile, toRunRequest(userId, request)).toResponseMap();
    }

    public Flux<String> stream(String userId, ReActChatRequest request) {
        return reActEngine.stream(learnReActProfile, toRunRequest(userId, request));
    }

    public ReActTraceDocument getTrace(String userId, String traceId) {
        return reActEngine.getTrace(userId, traceId, learnReActProfile.domain());
    }

    public List<ReActTraceDocument> listTraces(String userId, String sessionId) {
        return reActEngine.listTraces(userId, learnReActProfile.domain(), sessionId);
    }

    public Map<String, Object> intervene(String userId, String traceId, Integer stepNumber, String message) {
        return reActEngine.intervene(userId, traceId, learnReActProfile.domain(), stepNumber, message);
    }

    public Map<String, Object> stop(String userId, String traceId) {
        return reActEngine.stop(userId, traceId, learnReActProfile.domain());
    }

    private ReActRunRequest toRunRequest(String userId, ReActChatRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("autoCreateNote", Boolean.TRUE.equals(request.getAutoCreateNote()));
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = request.getSessionId();
        }
        return ReActRunRequest.builder()
                .userId(userId)
                .sessionId(conversationId)
                .message(request.getMessage())
                .strategy(request.getStrategy())
                .stream(request.getStream())
                .memoryEnabled(request.getMemoryEnabled())
                .maxIterations(request.getMaxIterations())
                .autoCreateNote(request.getAutoCreateNote())
                .tags(request.getTags() == null ? List.of() : request.getTags())
                .category(request.getCategory())
                .noteIds(request.getNoteIds() == null ? List.of() : request.getNoteIds())
                .metadata(metadata)
                .build();
    }
}
