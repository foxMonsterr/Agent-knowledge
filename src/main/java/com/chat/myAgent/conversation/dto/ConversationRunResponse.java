package com.chat.myAgent.conversation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ConversationRunResponse {
    private String conversationId;
    private String traceId;
    private String agentType;
    private String mode;
    private String reply;
    private String thinking;
    private String model;
    private Boolean memoryEnabled;
    private List<String> sources;
    private Integer retrievedChunks;
    private List<ConversationToolCallVO> toolCalls;
    private Boolean planned;
    private List<?> steps;
    private Long totalTimeMs;
    private String status;
    private Map<String, Object> metadata;
}
