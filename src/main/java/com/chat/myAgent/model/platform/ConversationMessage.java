package com.chat.myAgent.model.platform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一消息模型
 *
 * 用于记录一次对话中的消息内容，后续可以用于：
 * - 普通聊天
 * - 记忆对话
 * - 工具调用轨迹
 * - RAG 上下文
 * - 审计日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 所属会话 ID
     */
    private String conversationId;

    /**
     * 消息角色，例如 system / user / assistant / tool
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 是否包含思考内容
     */
    private boolean thinkingEnabled;

    /**
     * 思考内容
     */
    private String thinking;

    /**
     * 使用模型
     */
    private String model;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
