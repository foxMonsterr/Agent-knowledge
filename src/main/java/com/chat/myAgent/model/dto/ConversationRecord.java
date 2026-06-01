package com.chat.myAgent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话记录 DTO
 *
 * 用于封装 TokenUsageTracker.trackConversation() 方法的参数
 * 避免方法参数过多，提高代码可读性和可维护性
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRecord {

    /**
     * 会话 ID（用于关联同一会话的多轮对话）
     */
    private String conversationId;

    /**
     * 用户名（调用者身份）
     */
    private String username;

    /**
     * 用户消息内容
     */
    private String userMessage;

    /**
     * AI 回复内容
     */
    private String aiReply;

    /**
     * Agent 类型（如 ChatAgent、ToolAgent、RagAgent 等）
     */
    private String agentType;

    /**
     * 使用的模型名称（如 deepseek-v4-flash）
     */
    private String model;

    /**
     * 输入 Token 数量（用户消息 + 历史消息 + 系统提示词）
     */
    private Long promptTokens;

    /**
     * 输出 Token 数量（AI 生成的回复）
     */
    private Long completionTokens;

    /**
     * 响应耗时（毫秒）
     */
    private Long latencyMs;
}
