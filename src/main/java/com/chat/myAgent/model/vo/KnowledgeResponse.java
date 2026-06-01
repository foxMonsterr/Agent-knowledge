package com.chat.myAgent.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 知识库问答响应体
 */
@Data
@Builder
public class KnowledgeResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * AI 基于知识库的回答
     */
    private String answer;

    /**
     * 引用的来源文件列表
     */
    private List<String> sources;

    /**
     * 检索到的文档片段数量
     */
    private Integer retrievedChunks;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 请求追踪ID
     */
    private String traceId;
}
