package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeSearchRequest {
    @NotBlank(message = "检索内容不能为空")
    private String query;
    private Integer topK = 5;
    private Double threshold = 0.5;
    private String scope = "all";
    private List<String> tags;
    private String category;
    private List<String> noteIds;
}
