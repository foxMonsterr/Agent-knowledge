package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeynmanEvaluateRequest {
    @NotBlank(message = "noteId不能为空")
    private String noteId;
    @NotBlank(message = "解释内容不能为空")
    private String explanation;
}
