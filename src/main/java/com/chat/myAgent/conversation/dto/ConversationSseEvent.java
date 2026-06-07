package com.chat.myAgent.conversation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationSseEvent<T> {
    private String eventId;
    private String type;
    private String conversationId;
    private String traceId;
    private String agentType;
    private String mode;
    private String timestamp;
    private T data;
}
