package com.chat.myAgent.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Agent 响应体
 */
@Data
@Builder
public class AgentResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * AI 回复内容
     */
    private String reply;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * Agent 类型标识
     * tool: 工具调用（无记忆）
     * tool-memory: 工具调用（带记忆）
     * tool-specific: 指定工具调用
     */
    private String agentType;

    /**
     * 思考过程
     */
    private String thinking;

    /**
     * 本次响应关联的 traceId
     */
    private String traceId;
}
