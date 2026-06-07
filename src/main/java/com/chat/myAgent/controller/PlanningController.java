package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.conversation.dto.ConversationRunRequest;
import com.chat.myAgent.conversation.dto.ConversationRunResponse;
import com.chat.myAgent.conversation.service.ConversationAgentRouter;
import com.chat.myAgent.model.dto.AgentRequest;
import com.chat.myAgent.model.dto.PlanningRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 任务规划 + 全能Agent 接口
 *
 * @deprecated 此类已废弃，请使用统一的会话接口。
 * 迁移路径：使用 {@code POST /api/v1/conversations/chat}，设置 agentType="planning" 或 "full"。
 * 流式输出使用 {@code GET /api/v1/conversations/chat/stream}，设置相应的 agentType 和 mode。
 */
@Deprecated
@Tag(name = "任务规划", description = "复杂任务拆解规划 + 全能Agent统一入口（已废弃，请使用 /api/v1/conversations）")
@RestController
@RequestMapping("/api/v1/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final ConversationAgentRouter conversationAgentRouter;

    /**
     * 任务规划并执行
     * POST /api/v1/planning/execute
     *
     * 测试用例：
     * - 简单: "今天星期几" → 不规划，直接回答
     * - 复杂: "读取sample.md文件，总结核心内容，然后翻译成英文" → 拆解3步执行
     * - 复杂: "查一下技术部有哪些人，算一下他们的平均工资，然后告诉我现在几点了" → 拆解3步
     */
    @Operation(summary = "任务规划并执行", description = "AI自动判断是否需要规划，复杂任务拆解为多步执行")
    @PostMapping("/execute")
    public R<ConversationRunResponse> planAndExecute(@Valid @RequestBody PlanningRequest request) {
        return R.ok(conversationAgentRouter.chat(toRunRequest(request, "planning")));
    }

    @Operation(summary = "任务规划流式输出", description = "SSE流式返回规划过程与结果")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> planStream(@Parameter(description = "任务内容") @RequestParam(required = false) String message,
                                   @Parameter(description = "兼容旧参数：任务内容") @RequestParam(required = false) String task,
                                   @Parameter(description = "会话ID") @RequestParam(required = false) String conversationId,
                                   @Parameter(description = "是否自动执行") @RequestParam(defaultValue = "true") boolean autoExecute,
                                   @Parameter(description = "模式") @RequestParam(required = false) String mode,
                                   @RequestParam(required = false, defaultValue = "true") boolean memoryEnabled) {
        String resolvedMessage = resolveRequiredText(message, task, "message/task");
        ConversationRunRequest request = new ConversationRunRequest();
        request.setMessage(resolvedMessage);
        request.setConversationId(conversationId);
        request.setAgentType("planning");
        request.setMode(mode == null || mode.isBlank() ? "planning" : mode);
        request.setAutoExecute(autoExecute);
        request.setMemoryEnabled(memoryEnabled);
        request.setStream(true);
        return conversationAgentRouter.stream(request);
    }

    /**
     * 仅规划不执行
     * POST /api/v1/planning/plan-only
     *
     * 只返回任务拆解结果，不实际执行步骤
     */
    @Operation(summary = "仅规划不执行", description = "只返回任务拆解结果，不实际执行步骤")
    @PostMapping("/plan-only")
    public R<ConversationRunResponse> planOnly(@Valid @RequestBody PlanningRequest request) {
        request.setAutoExecute(false);
        return R.ok(conversationAgentRouter.chat(toRunRequest(request, "planning_only")));
    }

    /**
     * 全能Agent统一入口
     * POST /api/v1/planning/chat
     *
     * 整合记忆+工具的统一对话入口
     */
    @Operation(summary = "全能Agent统一入口", description = "整合记忆+工具的统一对话入口")
    @PostMapping("/chat")
    public R<ConversationRunResponse> fullChat(@Valid @RequestBody AgentRequest request) {
        ConversationRunRequest runRequest = new ConversationRunRequest();
        runRequest.setConversationId(request.getConversationId());
        runRequest.setMessage(request.getMessage());
        runRequest.setAgentType("full");
        runRequest.setMode("chat");
        runRequest.setTools(request.getTools());
        runRequest.setThinkingMode(request.getThinkingMode());
        runRequest.setMemoryEnabled(request.getMemoryEnabled());
        return R.ok(conversationAgentRouter.chat(runRequest));
    }

    private ConversationRunRequest toRunRequest(PlanningRequest request, String mode) {
        ConversationRunRequest runRequest = new ConversationRunRequest();
        runRequest.setConversationId(request.getConversationId());
        runRequest.setMessage(request.getTask());
        runRequest.setAgentType("planning");
        runRequest.setMode(mode);
        runRequest.setAutoExecute(request.getAutoExecute());
        runRequest.setThinkingMode(request.getThinkingMode());
        runRequest.setMemoryEnabled(request.getMemoryEnabled());
        return runRequest;
    }

    private String resolveRequiredText(String primary, String fallback, String fieldName) {
        String resolved = primary != null && !primary.isBlank() ? primary : fallback;
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return resolved;
    }
}
