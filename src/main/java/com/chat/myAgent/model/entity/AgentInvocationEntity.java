package com.chat.myAgent.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 执行记录实体
 *
 * 用于持久化一次 Agent 执行过程，便于审计、监控与回放。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_agent_invocation", indexes = {
        @Index(name = "idx_agent_conversation_id", columnList = "conversationId"),
        @Index(name = "idx_agent_type", columnList = "agentType"),
        @Index(name = "idx_agent_trace_id", columnList = "traceId")
})
public class AgentInvocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String invocationId;

    @Column(nullable = false, length = 64)
    private String conversationId;

    @Column(length = 30)
    private String agentType;

    @Column(length = 80)
    private String model;

    @Column(length = 64)
    private String traceId;

    @Column(columnDefinition = "TEXT")
    private String inputText;

    @Column(columnDefinition = "TEXT")
    private String outputText;

    @Column(columnDefinition = "TEXT")
    private String thinkingText;

    @Column(length = 20)
    private String status;

    private Long latencyMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
