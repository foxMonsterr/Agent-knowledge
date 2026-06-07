package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterventionRequest {
    @NotBlank(message = "traceId不能为空")
    private String traceId;
    private Integer stepNumber;
    @NotBlank(message = "补充信息不能为空")
    private String message;
}
