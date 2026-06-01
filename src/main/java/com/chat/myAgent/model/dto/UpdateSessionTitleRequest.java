package com.chat.myAgent.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSessionTitleRequest {

    @NotBlank(message = "标题不能为空")
    private String title;
}
