package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlashcardGenerateRequest {
    @NotBlank(message = "noteId不能为空")
    private String noteId;
    private Integer count = 10;
    private String style = "qa";
}
