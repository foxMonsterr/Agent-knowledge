package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStageRequest {
    @NotBlank(message = "阶段ID不能为空")
    private String stageId;

    private Integer score;
    private String status;
}
