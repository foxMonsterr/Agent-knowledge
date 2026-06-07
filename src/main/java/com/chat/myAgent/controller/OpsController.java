package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.entity.AgentInvocationEntity;
import com.chat.myAgent.model.vo.*;
import com.chat.myAgent.repository.AgentInvocationRepository;
import com.chat.myAgent.repository.ChatHistoryRepository;
import com.chat.myAgent.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 运营看板接口
 */
@Tag(name = "运营看板", description = "关键运营指标与趋势")
@RestController
@RequestMapping("/api/v1/ops")
@RequiredArgsConstructor
public class OpsController {

    private final UserRepository userRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final AgentInvocationRepository agentInvocationRepository;

    @Operation(summary = "获取运营指标")
    @GetMapping("/metrics")
    public R<List<OpsMetricVO>> metrics() {
        long users = userRepository.count();
        long chats = chatHistoryRepository.count();
        long runs = agentInvocationRepository.count();
        long tokens = chatHistoryRepository.sumTotalTokens() == null ? 0 : chatHistoryRepository.sumTotalTokens();

        return R.ok(List.of(
                OpsMetricVO.builder().title("总用户数").value(String.valueOf(users)).subTitle("平台注册用户").trend("stable").status("good").build(),
                OpsMetricVO.builder().title("聊天记录").value(String.valueOf(chats)).subTitle("历史对话条数").trend("up").status("good").build(),
                OpsMetricVO.builder().title("Agent 调用").value(String.valueOf(runs)).subTitle("工具/规划/RAG 执行次数").trend("up").status("good").build(),
                OpsMetricVO.builder().title("Token 总量").value(String.valueOf(tokens)).subTitle("累计 token 消耗").trend("up").status("warning").build()
        ));
    }

    @Operation(summary = "获取运营看板")
    @GetMapping("/dashboard")
    public R<OpsDashboardVO> dashboard() {
        List<OpsMetricVO> metrics = metrics().getData();
        List<TrendPointVO> dailyChats = buildTrend(7, "chats");
        List<TrendPointVO> dailyRuns = buildTrendRuns(7);
        List<TrendPointVO> dailyTokens = buildTrendTokens(7);
        List<AuditLogVO> recentAudits = agentInvocationRepository.findTop8ByOrderByCreatedAtDesc().stream()
                .map(this::toAuditVO)
                .toList();

        return R.ok(OpsDashboardVO.builder()
                .metrics(metrics)
                .dailyChats(dailyChats)
                .dailyRuns(dailyRuns)
                .dailyTokens(dailyTokens)
                .recentAudits(recentAudits)
                .build());
    }

    private List<TrendPointVO> buildTrend(int days, String type) {
        return java.util.stream.IntStream.range(0, days)
                .mapToObj(i -> {
                    LocalDate day = LocalDate.now().minusDays(days - i - 1L);
                    long count = 0;
                    if ("chats".equals(type)) {
                        count = chatHistoryRepository.countByCreatedAtBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
                    }
                    return TrendPointVO.builder().label(day.format(DateTimeFormatter.ofPattern("MM-dd"))).value(count).build();
                }).toList();
    }

    private List<TrendPointVO> buildTrendRuns(int days) {
        return java.util.stream.IntStream.range(0, days)
                .mapToObj(i -> {
                    LocalDate day = LocalDate.now().minusDays(days - i - 1L);
                    long count = agentInvocationRepository.countByCreatedAtBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
                    return TrendPointVO.builder().label(day.format(DateTimeFormatter.ofPattern("MM-dd"))).value(count).build();
                }).toList();
    }

    private List<TrendPointVO> buildTrendTokens(int days) {
        return java.util.stream.IntStream.range(0, days)
                .mapToObj(i -> {
                    LocalDate day = LocalDate.now().minusDays(days - i - 1L);
                    long total = chatHistoryRepository.sumTokensByCreatedAtBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
                    return TrendPointVO.builder().label(day.format(DateTimeFormatter.ofPattern("MM-dd"))).value(total).build();
                }).toList();
    }

    private AuditLogVO toAuditVO(AgentInvocationEntity e) {
        return AuditLogVO.builder()
                .traceId(e.getTraceId())
                .conversationId(e.getConversationId())
                .agentType(e.getAgentType())
                .model(e.getModel())
                .input(e.getInputText())
                .output(e.getOutputText())
                .thinking(e.getThinkingText())
                .status(e.getStatus())
                .latencyMs(e.getLatencyMs())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
