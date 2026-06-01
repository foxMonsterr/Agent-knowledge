package com.chat.myAgent.model.platform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 执行记录
 *
 * 记录一次 Agent 从接收请求到输出结果的完整执行过程，后续可用于：
 * - 监控
 * - 审计
 * - 调试
 * - 可视化执行轨迹
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunRecord {

    /**
     * 运行 ID
     */
    private String runId;

    /**
     * 所属会话 ID
     */
    private String conversationId;

    /**
     * Agent 类型，例如 chat / tool / rag / planning / stream
     */
    private String agentType;

    /**
     * 调用的模型名
     */
    private String model;

    /**
     * 输入内容
     */
    private String input;

    /**
     * 输出内容
     */
    private String output;

    /**
     * 思考内容
     */
    private String thinking;

    /**
     * 使用的工具列表
     */
    private List<String> tools;

    /**
     * 运行状态，例如 STARTED / SUCCESS / FAILED
     */
    private String status;

    /**
     * 总耗时（毫秒）
     */
    private long latencyMs;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
