package com.chat.myAgent.conversation.core;

public enum ConversationMode {
    CHAT("chat"),
    MEMORY("memory"),
    SPECIFIC("specific"),
    TOOLS("tools"),
    RAG("rag"),
    RAG_MANUAL("rag_manual"),
    PLANNING("planning"),
    PLANNING_ONLY("planning_only"),
    LEARN("learn"),
    REACT("react");

    private final String value;

    ConversationMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ConversationMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return CHAT;
        }
        String normalized = raw.trim().toLowerCase();
        for (ConversationMode item : values()) {
            if (item.value.equals(normalized) || item.name().equalsIgnoreCase(normalized.replace("-", "_"))) {
                return item;
            }
        }
        return CHAT;
    }
}
