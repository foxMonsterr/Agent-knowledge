package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ReActChatRequest {
    private String conversationId;
    private String sessionId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private String strategy = null;
    private List<String> noteIds;
    private List<String> tags;
    private String category;
    private Boolean stream = false;
    private Integer maxIterations = 6;
    private Boolean autoCreateNote = false;
    private Boolean memoryEnabled = true;
}
