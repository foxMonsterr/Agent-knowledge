package com.chat.myAgent.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话历史实体
 *
 * 记录每一轮对话，用于审计、分析、持久化
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_chat_history", indexes = {
        @Index(name = "idx_conversation_id", columnList = "conversationId"),
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
public class ChatHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话ID
     */
    @Column(nullable = false, length = 64)
    private String conversationId;

    /**
     * 用户名
     */
    @Column(nullable = false, length = 50)
    private String username;

    /**
     * 消息角色：user / assistant / system
     */
    @Column(nullable = false, length = 20)
    private String messageRole;

    /**
     * 消息内容
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Agent 类型：simple / memory / tool / rag / planning / full
     */
    @Column(length = 30)
    private String agentType;

    /**
     * 使用的模型
     */
    @Column(length = 50)
    private String model;

    /**
     * Prompt Token 数
     */
    private Long promptTokens;

    /**
     * Completion Token 数
     */
    private Long completionTokens;

    /**
     * 响应耗时（毫秒）
     */
    private Long latencyMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
