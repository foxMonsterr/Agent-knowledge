package com.chat.myAgent.controller;

import com.chat.myAgent.agent.ChatAgent;
import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.dto.ChatRequest;
import com.chat.myAgent.model.vo.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "对话管理", description = "多轮记忆对话 + 流式输出")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatAgent chatAgent;

    @Operation(summary = "多轮记忆对话")
    @PostMapping("/memory")
    public R<ChatResponse> memoryChat(@Valid @RequestBody ChatRequest request) {
        return R.ok(chatAgent.chat(request));
    }

    @Operation(summary = "多轮记忆流式对话")
    @GetMapping(value = "/memory/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> memoryChatStream(
            @Parameter(description = "对话消息", required = true) @RequestParam String message,
            @Parameter(description = "会话ID") @RequestParam(required = false) String conversationId) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        return chatAgent.memoryChatStream(request);
    }

    @Operation(summary = "健康检查")
    @GetMapping("/ping")
    public R<String> ping() {
        return R.ok("SmartAgent is running! 🚀");
    }
}
