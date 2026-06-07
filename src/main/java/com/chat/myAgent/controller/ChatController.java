package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.conversation.dto.ConversationRunRequest;
import com.chat.myAgent.conversation.dto.ConversationRunResponse;
import com.chat.myAgent.conversation.service.ConversationAgentRouter;
import com.chat.myAgent.model.dto.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @deprecated 此类已废弃，请使用统一的会话接口。
 * 迁移路径：使用 {@code POST /api/v1/conversations/chat}，设置 agentType="chat", mode="memory"。
 * 流式输出使用 {@code GET /api/v1/conversations/chat/stream}，设置 agentType="chat", mode="memory"。
 */
@Deprecated
@Tag(name = "对话管理", description = "多轮记忆对话 + 流式输出（已废弃，请使用 /api/v1/conversations）")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ConversationAgentRouter conversationAgentRouter;

    @Operation(summary = "多轮记忆对话")
    @PostMapping("/memory")
    public R<ConversationRunResponse> memoryChat(@Valid @RequestBody ChatRequest request) {
        ConversationRunRequest runRequest = new ConversationRunRequest();
        runRequest.setConversationId(request.getConversationId());
        runRequest.setMessage(request.getMessage());
        runRequest.setAgentType("chat");
        runRequest.setMode("memory");
        runRequest.setMemoryEnabled(request.getMemoryEnabled());
        runRequest.setThinkingMode(request.getThinkingMode());
        runRequest.setModel(request.getModel());
        runRequest.setRole(request.getRole());
        runRequest.setLevel(request.getLevel());
        return R.ok(conversationAgentRouter.chat(runRequest));
    }

    @Operation(summary = "多轮记忆流式对话")
    @GetMapping(value = "/memory/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> memoryChatStream(
            @Parameter(description = "对话消息", required = true) @RequestParam String message,
            @Parameter(description = "会话ID") @RequestParam(required = false) String conversationId,
            @RequestParam(required = false, defaultValue = "true") boolean memoryEnabled) {
        ConversationRunRequest request = new ConversationRunRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        request.setAgentType("chat");
        request.setMode("memory");
        request.setMemoryEnabled(memoryEnabled);
        request.setStream(true);
        return conversationAgentRouter.stream(request);
    }

    @Operation(summary = "健康检查")
    @GetMapping("/ping")
    public R<String> ping() {
        return R.ok("SmartAgent is running! 🚀");
    }
}
