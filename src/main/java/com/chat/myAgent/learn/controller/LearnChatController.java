package com.chat.myAgent.learn.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.conversation.dto.ConversationRunRequest;
import com.chat.myAgent.conversation.dto.ConversationRunResponse;
import com.chat.myAgent.conversation.service.ConversationAgentRouter;
import com.chat.myAgent.learn.dto.InterventionRequest;
import com.chat.myAgent.learn.dto.ReActChatRequest;
import com.chat.myAgent.learn.dto.StopRequest;
import com.chat.myAgent.learn.service.LearnAgent;
import com.chat.myAgent.learn.service.LearnUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/learn/chat")
@RequiredArgsConstructor
public class LearnChatController {

    private final LearnAgent learnAgent;
    private final LearnUserService learnUserService;
    private final ConversationAgentRouter conversationAgentRouter;

    @PostMapping
    public R<ConversationRunResponse> chat(@Valid @RequestBody ReActChatRequest request) {
        return R.ok(conversationAgentRouter.chat(toRunRequest(request)));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message,
                               @RequestParam(required = false) String conversationId,
                               @RequestParam(required = false) String sessionId,
                               @RequestParam(required = false, defaultValue = "auto") String strategy,
                               @RequestParam(required = false, defaultValue = "true") boolean memoryEnabled) {
        ConversationRunRequest request = new ConversationRunRequest();
        request.setMessage(message);
        request.setConversationId(conversationId != null && !conversationId.isBlank() ? conversationId : sessionId);
        request.setAgentType("learn-react");
        request.setMode("learn");
        request.setStrategy(strategy);
        request.setMemoryEnabled(memoryEnabled);
        request.setStream(true);
        return conversationAgentRouter.stream(request);
    }

    @GetMapping("/traces/{traceId}")
    public R<?> trace(@PathVariable String traceId) {
        return R.ok(learnAgent.getTrace(learnUserService.currentUserId(), traceId));
    }

    @GetMapping("/traces")
    public R<?> traces(@RequestParam(required = false) String sessionId) {
        return R.ok(learnAgent.listTraces(learnUserService.currentUserId(), sessionId));
    }

    @PostMapping("/interventions")
    public R<Map<String, Object>> intervene(@Valid @RequestBody InterventionRequest request) {
        return R.ok(learnAgent.intervene(learnUserService.currentUserId(), request.getTraceId(),
                request.getStepNumber(), request.getMessage()));
    }

    @PostMapping("/stop")
    public R<Map<String, Object>> stop(@Valid @RequestBody StopRequest request) {
        return R.ok(learnAgent.stop(learnUserService.currentUserId(), request.getTraceId()));
    }

    private ConversationRunRequest toRunRequest(ReActChatRequest request) {
        ConversationRunRequest runRequest = new ConversationRunRequest();
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = request.getSessionId();
        }
        runRequest.setConversationId(conversationId);
        runRequest.setMessage(request.getMessage());
        runRequest.setAgentType("learn-react");
        runRequest.setMode("learn");
        runRequest.setStrategy(request.getStrategy());
        runRequest.setNoteIds(request.getNoteIds());
        runRequest.setTags(request.getTags());
        runRequest.setCategory(request.getCategory());
        runRequest.setMaxIterations(request.getMaxIterations());
        runRequest.setAutoCreateNote(request.getAutoCreateNote());
        runRequest.setMemoryEnabled(request.getMemoryEnabled());
        return runRequest;
    }
}
