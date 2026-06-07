package com.chat.myAgent.conversation.core;

public enum ConversationAgentType {
    CHAT("chat"),
    TOOL("tool"),
    RAG("rag"),
    PLANNING("planning"),
    FULL("full"),
    LEARN_REACT("learn-react"),
    GENERAL_REACT("general-react");

    private final String value;

    ConversationAgentType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ConversationAgentType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return CHAT;
        }
        String normalized = raw.trim().toLowerCase();
        for (ConversationAgentType item : values()) {
            if (item.value.equals(normalized) || item.name().equalsIgnoreCase(normalized.replace("-", "_"))) {
                return item;
            }
        }
        return CHAT;
    }
}
