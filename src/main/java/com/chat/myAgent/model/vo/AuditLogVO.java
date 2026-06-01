package com.chat.myAgent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogVO {
    private String traceId;
    private String conversationId;
    private String agentType;
    private String model;
    private String input;
    private String output;
    private String thinking;
    private String status;
    private Long latencyMs;
    private LocalDateTime createdAt;
}
