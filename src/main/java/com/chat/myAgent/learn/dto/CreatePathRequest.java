package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePathRequest {
    @NotBlank(message = "学习主题不能为空")
    private String topic;

    private Integer targetNoteCount = 5;
    private String preferredDepth = "medium";
}
