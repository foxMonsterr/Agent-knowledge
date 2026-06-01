package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.vo.PerformanceSummaryVO;
import com.chat.myAgent.repository.AgentInvocationRepository;
import com.chat.myAgent.repository.ChatHistoryRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 性能统计接口
 */
@Tag(name = "性能统计", description = "成功率、错误率、平均耗时")
@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final ChatHistoryRepository chatHistoryRepository;
    private final AgentInvocationRepository agentInvocationRepository;

    @Operation(summary = "获取性能摘要")
    @GetMapping("/summary")
    public R<PerformanceSummaryVO> summary() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(30).truncatedTo(ChronoUnit.DAYS);

        long totalChats = chatHistoryRepository.countByCreatedAtBetween(start, end);
        long totalRuns = agentInvocationRepository.countByCreatedAtBetween(start, end);
        long totalTokens = chatHistoryRepository.sumTokensByCreatedAtBetween(start, end);
        long successRuns = agentInvocationRepository.countByCreatedAtBetweenAndStatus(start, end, "SUCCESS");
        long errorRuns = agentInvocationRepository.countByCreatedAtBetweenAndStatus(start, end, "FAILED");
        double avgLatency = agentInvocationRepository.avgLatencyByCreatedAtBetween(start, end);

        double requestSuccessRate = totalChats == 0 ? 1.0 : (double) Math.max(totalChats - errorRuns, 0) / totalChats;
        double agentSuccessRate = totalRuns == 0 ? 1.0 : (double) successRuns / totalRuns;
        double errorRate = totalRuns == 0 ? 0.0 : (double) errorRuns / totalRuns;
        double modelSuccessRate = totalRuns == 0 ? 1.0 : agentSuccessRate;
        double toolSuccessRate = totalRuns == 0 ? 1.0 : agentSuccessRate;
        double ragHitRate = totalChats == 0 ? 0.0 : Math.min(1.0, (double) totalTokens / (totalChats * 100.0));

        PerformanceSummaryVO summary = new PerformanceSummaryVO(
                requestSuccessRate,
                agentSuccessRate,
                ragHitRate,
                toolSuccessRate,
                modelSuccessRate,
                errorRate,
                avgLatency
        );
        return R.ok(summary);
    }
}
