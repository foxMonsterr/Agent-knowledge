package com.chat.myAgent.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 任务规划请求体
 */
@Data
public class PlanningRequest {

    /**
     * 用户需求描述
     */
    @NotBlank(message = "任务描述不能为空")
    private String task;

    /**
     * 会话ID（可选）
     */
    private String conversationId;

    /**
     * 是否自动执行规划结果（默认true）
     * false: 只返回规划，不执行
     * true: 规划后自动执行每个步骤
     */
    private Boolean autoExecute = true;

    /**
     * 是否开启思考模式
     */
    private Boolean thinkingMode = false;

    /**
     * 是否启用会话记忆，默认开启
     */
    private Boolean memoryEnabled = true;
}
