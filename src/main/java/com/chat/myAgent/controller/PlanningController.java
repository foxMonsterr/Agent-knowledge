package com.chat.myAgent.controller;

import com.chat.myAgent.agent.FullAgent;
import com.chat.myAgent.agent.PlanningAgent;
import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.dto.AgentRequest;
import com.chat.myAgent.model.dto.PlanningRequest;
import com.chat.myAgent.model.vo.AgentResponse;
import com.chat.myAgent.model.vo.PlanningResponse;
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
 */
@Tag(name = "任务规划", description = "复杂任务拆解规划 + 全能Agent统一入口")
@RestController
@RequestMapping("/api/v1/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final PlanningAgent planningAgent;
    private final FullAgent fullAgent;

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
    public R<PlanningResponse> planAndExecute(@Valid @RequestBody PlanningRequest request) {
        PlanningResponse response = planningAgent.planAndExecute(
                request.getTask(),
                request.getConversationId(),
                request.getAutoExecute() != null ? request.getAutoExecute() : true
        );
        return R.ok(response);
    }

    @Operation(summary = "任务规划流式输出", description = "SSE流式返回规划过程与结果")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> planStream(@Parameter(description = "任务内容", required = true) @RequestParam("message") String message,
                                   @Parameter(description = "会话ID") @RequestParam(required = false) String conversationId,
                                   @Parameter(description = "是否自动执行") @RequestParam(defaultValue = "true") boolean autoExecute,
                                   @Parameter(description = "模式") @RequestParam(required = false) String mode) {
        return planningAgent.planStream(message, conversationId, autoExecute, mode);
    }

    /**
     * 仅规划不执行
     * POST /api/v1/planning/plan-only
     *
     * 只返回任务拆解结果，不实际执行步骤
     */
    @Operation(summary = "仅规划不执行", description = "只返回任务拆解结果，不实际执行步骤")
    @PostMapping("/plan-only")
    public R<PlanningResponse> planOnly(@Valid @RequestBody PlanningRequest request) {
        PlanningResponse response = planningAgent.planAndExecute(
                request.getTask(),
                request.getConversationId(),
                false
        );
        return R.ok(response);
    }

    /**
     * 全能Agent统一入口
     * POST /api/v1/planning/chat
     *
     * 整合记忆+工具的统一对话入口
     */
    @Operation(summary = "全能Agent统一入口", description = "整合记忆+工具的统一对话入口")
    @PostMapping("/chat")
    public R<AgentResponse> fullChat(@Valid @RequestBody AgentRequest request) {
        AgentResponse response = fullAgent.chat(
                request.getMessage(),
                request.getConversationId()
        );
        return R.ok(response);
    }
}
