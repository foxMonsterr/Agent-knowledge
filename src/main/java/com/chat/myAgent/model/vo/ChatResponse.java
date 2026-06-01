package com.chat.myAgent.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 聊天响应体
 */
@Data
@Builder
public class ChatResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * AI 回复内容
     */
    private String reply;

    /**
     * 思考过程（如果模型返回了 reasoning/thinking 内容）
     */
    private String thinking;

    /**
     * 本次响应关联的 traceId
     */
    private String traceId;

    /**
     * 使用的模型名称
     */
    private String model;

    /**
     * 当前会话历史消息数量
     */
    private Integer historySize;

    /**
     * Token 使用情况（扩展预留）
     */
    private TokenUsage tokenUsage;

    @Data
    @Builder
    public static class TokenUsage {
        private Long promptTokens;
        private Long completionTokens;
        private Long totalTokens;
    }
}
