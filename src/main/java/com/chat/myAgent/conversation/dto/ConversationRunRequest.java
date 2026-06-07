package com.chat.myAgent.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ConversationRunRequest {
    private String conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private String agentType = "chat";
    private String mode = "chat";
    private List<String> tools;
    private Boolean thinkingMode = false;
    private Boolean memoryEnabled = true;
    private Boolean stream = false;
    private String model;
    private String role;
    private String level;
    private Integer topK;
    private Double similarityThreshold;
    private Boolean autoExecute = true;
    private String strategy = "auto";
    private List<String> noteIds;
    private List<String> tags;
    private String category;
    private Integer maxIterations = 6;
    private Boolean autoCreateNote = false;
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
