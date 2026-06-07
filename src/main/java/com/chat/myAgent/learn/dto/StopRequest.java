package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StopRequest {
    @NotBlank(message = "traceId不能为空")
    private String traceId;
}
