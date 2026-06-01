package com.chat.myAgent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 性能概览
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceSummaryVO {
    private double requestSuccessRate;
    private double agentSuccessRate;
    private double ragHitRate;
    private double toolSuccessRate;
    private double modelSuccessRate;
    private double errorRate;
    private double avgLatencyMs;
}
