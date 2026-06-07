package com.chat.myAgent.conversation.core;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ConversationContext {
    private String userId;
    private String username;
    private String conversationId;
    private String traceId;
    private ConversationAgentType agentType;
    private ConversationMode mode;
    private String message;
    private boolean memoryEnabled;
    private boolean stream;
    private boolean thinkingMode;
    private List<String> tools;
    private LocalDateTime startedAt;

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public String agentTypeValue() {
        return agentType == null ? ConversationAgentType.CHAT.value() : agentType.value();
    }

    public String modeValue() {
        return mode == null ? ConversationMode.CHAT.value() : mode.value();
    }
}
