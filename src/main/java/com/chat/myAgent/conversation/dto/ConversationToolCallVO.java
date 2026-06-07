package com.chat.myAgent.conversation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ConversationToolCallVO {
    private String toolName;
    private Map<String, Object> input;
    private Object output;
    private String status;
    private Long durationMs;
    private String errorMessage;
}
