package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.entity.ChatHistoryEntity;
import com.chat.myAgent.model.vo.MonitorOverviewVO;
import com.chat.myAgent.monitor.TokenUsageTracker;
import com.chat.myAgent.repository.ChatHistoryRepository;
import com.chat.myAgent.repository.UserRepository;
import com.chat.myAgent.repository.AgentInvocationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控管理接口
 *
 * 提供 Token 消耗统计、对话历史查询、系统运行状态
 * 需要 ADMIN 角色
 */
@Tag(name = "系统监控", description = "Token统计、对话历史、系统状态")
@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final TokenUsageTracker tokenUsageTracker;
    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final AgentInvocationRepository agentInvocationRepository;

    @Operation(summary = "获取系统运行统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("session", tokenUsageTracker.getSessionStatistics());
        stats.put("allTime", tokenUsageTracker.getAllTimeStatistics());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalChatRecords", chatHistoryRepository.count());
        stats.put("totalAgentRuns", agentInvocationRepository.count());
        return R.ok(stats);
    }

    @Operation(summary = "获取监控概览")
    @GetMapping("/overview")
    public R<MonitorOverviewVO> overview() {
        long promptTokens = chatHistoryRepository.sumPromptTokens() == null ? 0 : chatHistoryRepository.sumPromptTokens();
        long completionTokens = chatHistoryRepository.sumCompletionTokens() == null ? 0 : chatHistoryRepository.sumCompletionTokens();
        long totalTokens = chatHistoryRepository.sumTotalTokens() == null ? 0 : chatHistoryRepository.sumTotalTokens();
        MonitorOverviewVO overview = MonitorOverviewVO.builder()
                .totalUsers(userRepository.count())
                .totalChatRecords(chatHistoryRepository.count())
                .totalAgentRuns(agentInvocationRepository.count())
                .totalTokens(totalTokens)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build();
        return R.ok(overview);
    }

    @Operation(summary = "查询对话历史")
    @GetMapping("/history")
    public R<Page<ChatHistoryEntity>> getHistory(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ChatHistoryEntity> result;
        if (username != null && !username.isBlank()) {
            result = chatHistoryRepository.findByUsernameOrderByCreatedAtDesc(username, pageRequest);
        } else {
            result = chatHistoryRepository.findAll(pageRequest);
        }

        return R.ok(result);
    }

    @Operation(summary = "查询指定会话的完整对话")
    @GetMapping("/conversation/{conversationId}")
    public R<List<ChatHistoryEntity>> getConversation(@PathVariable String conversationId) {
        List<ChatHistoryEntity> history = chatHistoryRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return R.ok(history);
    }

    @Operation(summary = "查询用户的所有会话列表")
    @GetMapping("/sessions/{username}")
    public R<List<String>> getUserSessions(@PathVariable String username) {
        List<String> sessions = chatHistoryRepository.findDistinctConversationIdsByUsername(username);
        return R.ok(sessions);
    }
}
