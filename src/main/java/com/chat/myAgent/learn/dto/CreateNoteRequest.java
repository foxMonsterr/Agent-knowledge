package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateNoteRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private List<String> tags;
    private String category;
    private String sourceTraceId;
    private Boolean vectorIndex = true;
}
