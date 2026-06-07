package com.chat.myAgent.controller;

import com.chat.myAgent.conversation.dto.ConversationRunRequest;
import com.chat.myAgent.conversation.service.ConversationAgentRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 流式对话接口
 *
 * 使用 SSE（Server-Sent Events）实时推送AI回复
 * 前端通过 EventSource 或 fetch + ReadableStream 接收
 *
 * @deprecated 此类已废弃，请使用统一的会话接口。
 * 迁移路径：使用 {@code GET /api/v1/conversations/chat/stream}，设置 agentType="chat" 或 "tool"，mode="chat" 或 "tools"。
 */
@Deprecated
@Tag(name = "流式对话", description = "SSE实时推送AI回复，支持基础对话和工具调用（已废弃，请使用 /api/v1/conversations/chat/stream）")
@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class StreamController {

    private final ConversationAgentRouter conversationAgentRouter;

    /**
     * 流式对话（基础版）
     * GET /api/v1/stream/chat?message=xxx&conversationId=xxx
     *
     * 返回 text/event-stream，浏览器可直接通过 EventSource 接收
     *
     * 浏览器测试：直接在地址栏输入
     * http://localhost:8080/api/v1/stream/chat?message=用200字介绍一下Spring框架
     */
    @Operation(summary = "流式对话（基础版）", description = "返回text/event-stream，浏览器可通过EventSource接收")
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @Parameter(description = "对话消息", required = true) @RequestParam String message,
            @Parameter(description = "会话ID") @RequestParam(required = false) String conversationId,
            @RequestParam(required = false, defaultValue = "true") boolean memoryEnabled) {
        ConversationRunRequest request = new ConversationRunRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        request.setAgentType("chat");
        request.setMode("chat");
        request.setMemoryEnabled(memoryEnabled);
        request.setStream(true);
        return conversationAgentRouter.stream(request);
    }

    /**
     * 流式对话（带工具）
     * GET /api/v1/stream/chat/tools?message=xxx
     */
    @Operation(summary = "流式对话（带工具）", description = "流式对话中AI可自主调用工具")
    @GetMapping(value = "/chat/tools", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChatWithTools(
            @Parameter(description = "对话消息", required = true) @RequestParam String message,
            @Parameter(description = "会话ID") @RequestParam(required = false) String conversationId,
            @RequestParam(required = false, defaultValue = "true") boolean memoryEnabled) {
        ConversationRunRequest request = new ConversationRunRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        request.setAgentType("tool");
        request.setMode("tools");
        request.setMemoryEnabled(memoryEnabled);
        request.setStream(true);
        return conversationAgentRouter.stream(request);
    }
}
