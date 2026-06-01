package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.vo.HomeOverviewVO;
import com.chat.myAgent.model.vo.OpsMetricVO;
import com.chat.myAgent.repository.AgentInvocationRepository;
import com.chat.myAgent.repository.ChatHistoryRepository;
import com.chat.myAgent.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页概览接口
 */
@Tag(name = "首页概览", description = "首页展示的产品信息、亮点与运营指标")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final AgentInvocationRepository agentInvocationRepository;

    @Operation(summary = "获取首页概览")
    @GetMapping("/overview")
    public R<HomeOverviewVO> overview() {
        long totalUsers = userRepository.count();
        long totalChatRecords = chatHistoryRepository.count();
        long totalAgentRuns = agentInvocationRepository.count();
        long totalTokens = chatHistoryRepository.sumTotalTokens() == null ? 0 : chatHistoryRepository.sumTotalTokens();

        List<OpsMetricVO> metrics = List.of(
                OpsMetricVO.builder().title("总用户数").value(String.valueOf(totalUsers)).subTitle("平台注册用户").build(),
                OpsMetricVO.builder().title("聊天记录").value(String.valueOf(totalChatRecords)).subTitle("累计会话消息").build(),
                OpsMetricVO.builder().title("Agent 调用").value(String.valueOf(totalAgentRuns)).subTitle("执行链路次数").build(),
                OpsMetricVO.builder().title("Token 总量").value(String.valueOf(totalTokens)).subTitle("累计 token 消耗").build()
        );

        HomeOverviewVO vo = HomeOverviewVO.builder()
                .title("企业级 AI 智能体平台")
                .subtitle("多 Agent 编排 · RAG 知识库 · 工具调用 · 审计监控")
                .metrics(metrics)
                .highlights(List.of(
                        "JWT + RBAC 权限体系",
                        "TraceId 全链路追踪",
                        "Docker 可部署",
                        "前后端分离控制台"
                ))
                .build();
        return R.ok(vo);
    }
}
