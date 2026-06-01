package com.chat.myAgent.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 任务规划响应体
 */
@Data
@Builder
public class PlanningResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 任务总结
     */
    private String taskSummary;

    /**
     * 是否发生任务规划
     */
    private boolean planned;

    /**
     * 简单问题的直接回答
     */
    private String directAnswer;

    /**
     * 规划步骤列表
     */
    private List<StepResult> steps;

    /**
     * 最终汇总回答
     */
    private String finalAnswer;

    /**
     * 总耗时
     */
    private long totalTimeMs;

    /**
     * 本次响应关联的 traceId
     */
    private String traceId;
}