package com.chat.myAgent.controller;

import com.chat.myAgent.agent.GeneralReActAgent;
import com.chat.myAgent.common.result.R;
import com.chat.myAgent.conversation.dto.ConversationRunRequest;
import com.chat.myAgent.conversation.dto.ConversationRunResponse;
import com.chat.myAgent.conversation.service.ConversationAgentRouter;
import com.chat.myAgent.learn.dto.InterventionRequest;
import com.chat.myAgent.learn.dto.StopRequest;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.model.dto.AgentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Agent 工具调用接口
 *
 * @deprecated 此类已废弃，请使用统一的会话接口。
 * 迁移路径：使用 {@code POST /api/v1/conversations/chat}，设置 agentType="tool" 或 "general-react"。
 * 流式输出使用 {@code GET /api/v1/conversations/chat/stream}，设置相应的 agentType 和 mode。
 */
@Deprecated
@Tag(name = "Agent工具调用", description = "AI自主决策调用工具：时间查询、计算器、翻译、文档解析、数据库查询（已废弃，请使用 /api/v1/conversations）")
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final GeneralReActAgent generalReActAgent;
    private final ConversationAgentRouter conversationAgentRouter;
    private final LearnUserService learnUserService;

    /**
     * 工具调用对话（无记忆）
     * POST /api/v1/agent/chat
     *
     * AI 会根据用户问题自主决定是否调用工具
     *
     */
    @Operation(summary = "工具调用对话（无记忆）", description = "AI根据问题自主决定是否调用工具，不保留上下文")
    @PostMapping("/chat")
    public R<ConversationRunResponse> chat(@Valid @RequestBody AgentRequest request) {
        return R.ok(conversationAgentRouter.chat(toRunRequest(request, "tool", "chat")));
    }

    /**
     * 工具调用对话（带记忆）
     * POST /api/v1/agent/chat/memory
     *
     * 在工具调用基础上，增加多轮记忆
     * 适合需要上下文的连续工具调用场景
     */
    @Operation(summary = "工具调用对话（带记忆）", description = "在工具调用基础上增加多轮记忆，适合需要上下文的连续工具调用场景")
    @PostMapping("/chat/memory")
    public R<ConversationRunResponse> chatWithMemory(@Valid @RequestBody AgentRequest request) {
        request.setMemoryEnabled(true);
        return R.ok(conversationAgentRouter.chat(toRunRequest(request, "tool", "memory")));
    }

    /**
     * 指定工具对话
     * POST /api/v1/agent/chat/specific
     *
     * 只启用请求中指定的工具，限制AI的工具调用范围
     *
     * 请求示例：
     * {
     *   "message": "128*256等于多少",
     *   "tools": ["calculator", "datetime"]
     * }
     */
    @Operation(summary = "指定工具对话", description = "只启用请求中指定的工具，限制AI的工具调用范围")
    @PostMapping("/chat/specific")
    public R<ConversationRunResponse> chatWithSpecificTools(@Valid @RequestBody AgentRequest request) {
        return R.ok(conversationAgentRouter.chat(toRunRequest(request, "tool", "specific")));
    }

    @Operation(summary = "Agent 流式对话", description = "thinkingMode=true 时返回 ReAct 推理链事件；否则返回普通工具流式输出")
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message,
                                   @RequestParam(required = false) String conversationId,
                                   @RequestParam(required = false, defaultValue = "chat") String mode,
                                   @RequestParam(required = false) String tools,
                                   @RequestParam(required = false, defaultValue = "false") boolean thinkingMode,
                                   @RequestParam(required = false, defaultValue = "true") boolean memoryEnabled) {
        ConversationRunRequest request = new ConversationRunRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        request.setAgentType(thinkingMode ? "general-react" : "tool");
        request.setMode(thinkingMode ? "react" : mode);
        request.setThinkingMode(thinkingMode);
        request.setTools(parseTools(tools));
        request.setMemoryEnabled(memoryEnabled);
        request.setStream(true);
        return conversationAgentRouter.stream(request);
    }

    @GetMapping("/chat/traces/{traceId}")
    public R<?> trace(@PathVariable String traceId) {
        return R.ok(generalReActAgent.getTrace(learnUserService.currentUserId(), traceId));
    }

    @GetMapping("/chat/traces")
    public R<?> traces(@RequestParam(required = false) String sessionId) {
        return R.ok(generalReActAgent.listTraces(learnUserService.currentUserId(), sessionId));
    }

    @PostMapping("/chat/interventions")
    public R<Map<String, Object>> intervene(@Valid @RequestBody InterventionRequest request) {
        return R.ok(generalReActAgent.intervene(learnUserService.currentUserId(), request.getTraceId(),
                request.getStepNumber(), request.getMessage()));
    }

    @PostMapping("/chat/stop")
    public R<Map<String, Object>> stop(@Valid @RequestBody StopRequest request) {
        return R.ok(generalReActAgent.stop(learnUserService.currentUserId(), request.getTraceId()));
    }

    private List<String> parseTools(String tools) {
        if (tools == null || tools.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tools.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private ConversationRunRequest toRunRequest(AgentRequest request, String agentType, String mode) {
        ConversationRunRequest runRequest = new ConversationRunRequest();
        runRequest.setConversationId(request.getConversationId());
        runRequest.setMessage(request.getMessage());
        runRequest.setAgentType(Boolean.TRUE.equals(request.getThinkingMode()) ? "general-react" : agentType);
        runRequest.setMode(Boolean.TRUE.equals(request.getThinkingMode()) ? "react" : mode);
        runRequest.setTools(request.getTools());
        runRequest.setThinkingMode(request.getThinkingMode());
        runRequest.setMemoryEnabled(request.getMemoryEnabled());
        return runRequest;
    }
}
