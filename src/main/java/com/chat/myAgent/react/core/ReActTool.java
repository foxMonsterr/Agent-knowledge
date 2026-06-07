package com.chat.myAgent.react.core;

import java.util.Map;

public interface ReActTool {

    String name();

    String displayName();

    String category();

    boolean supports(ReActRunRequest request);

    Map<String, Object> input(ReActRunRequest request);

    ReActToolResult execute(ReActRunRequest request, Map<String, Object> input);
}
