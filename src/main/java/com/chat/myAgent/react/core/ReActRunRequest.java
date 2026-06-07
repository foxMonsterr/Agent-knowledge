package com.chat.myAgent.react.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActRunRequest {
    private String userId;
    private String sessionId;
    private String message;
    private String strategy;
    private String domain;
    private String agentType;
    private Boolean stream;
    private Boolean memoryEnabled = true;
    private Integer maxIterations;
    private Boolean autoCreateNote;
    private String category;

    @Builder.Default
    private List<String> toolNames = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<String> noteIds = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
