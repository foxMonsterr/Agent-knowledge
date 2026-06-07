package com.chat.myAgent.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库问答请求体
 */
@Data
public class KnowledgeRequest {

    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    private String question;

    /**
     * 会话ID（可选，支持多轮追问）
     */
    private String conversationId;

    /**
     * 检索结果数量（可选，默认5）
     */
    private Integer topK;

    /**
     * 相似度阈值（可选，默认0.5）
     */
    private Double similarityThreshold;

    /**
     * 是否启用会话记忆，默认开启
     */
    private Boolean memoryEnabled = true;
}
