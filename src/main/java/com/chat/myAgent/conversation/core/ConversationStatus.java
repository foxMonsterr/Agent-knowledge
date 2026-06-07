package com.chat.myAgent.conversation.core;

public enum ConversationStatus {
    SUCCESS("success"),
    FAILED("failed"),
    STOPPED("stopped");

    private final String value;

    ConversationStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
