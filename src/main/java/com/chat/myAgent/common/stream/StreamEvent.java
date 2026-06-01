package com.chat.myAgent.common.stream;

public record StreamEvent(String type, String content) {
    public static StreamEvent start(String content) { return new StreamEvent("start", content); }
    public static StreamEvent delta(String content) { return new StreamEvent("delta", content); }
    public static StreamEvent message(String content) { return new StreamEvent("message", content); }
    public static StreamEvent done(String content) { return new StreamEvent("done", content); }
    public static StreamEvent error(String content) { return new StreamEvent("error", content); }

    public String toJson() {
        return "{\"type\":\"" + escape(type) + "\",\"content\":\"" + escape(content) + "\"}";
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
