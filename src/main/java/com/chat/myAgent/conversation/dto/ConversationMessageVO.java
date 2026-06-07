package com.chat.myAgent.conversation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationMessageVO {
    private String role;
    private String content;
    private String agentType;
    private String model;
    private String traceId;
    private Long latencyMs;
    private String createdAt;
}
