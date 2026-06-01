package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.entity.AgentInvocationEntity;
import com.chat.myAgent.model.vo.AuditLogVO;
import com.chat.myAgent.repository.AgentInvocationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审计日志接口
 */
@Tag(name = "审计日志", description = "查询 Agent 执行和对话审计记录")
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AgentInvocationRepository agentInvocationRepository;

    @Operation(summary = "查询审计日志")
    @GetMapping("/logs")
    public R<List<AuditLogVO>> logs(@RequestParam(required = false) String conversationId) {
        List<AgentInvocationEntity> entities = conversationId == null || conversationId.isBlank()
                ? agentInvocationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                : agentInvocationRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);

        List<AuditLogVO> result = entities.stream().map(e -> AuditLogVO.builder()
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
                .build()).toList();
        return R.ok(result);
    }
}
