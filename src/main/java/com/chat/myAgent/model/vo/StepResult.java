package com.chat.myAgent.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 任务规划中的单步结果
 */
@Data
@Builder
public class StepResult {

    /**
     * 步骤编号
     */
    private int stepNumber;

    /**
     * 步骤描述
     */
    private String description;

    /**
     * 使用的工具
     */
    private String toolUsed;

    /**
     * 步骤输出结果
     */
    private String result;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 单步耗时
     */
    private long timeMs;
}