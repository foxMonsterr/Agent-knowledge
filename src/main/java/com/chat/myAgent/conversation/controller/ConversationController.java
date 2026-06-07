package com.chat.myAgent.conversation.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.conversation.dto.ConversationRunRequest;
import com.chat.myAgent.conversation.dto.ConversationRunResponse;
import com.chat.myAgent.conversation.memory.ConversationMemoryService;
import com.chat.myAgent.conversation.service.ConversationAgentRouter;
import com.chat.myAgent.conversation.service.ConversationHistoryService;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.model.dto.CreateSessionRequest;
import com.chat.myAgent.model.dto.UpdateSessionTitleRequest;
import com.chat.myAgent.model.vo.SessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationAgentRouter router;
    private final ConversationHistoryService historyService;
    private final ConversationMemoryService memoryService;
    private final LearnUserService learnUserService;

    @PostMapping("/chat")
    public R<ConversationRunResponse> chat(@Valid @RequestBody ConversationRunRequest request) {
        request.setStream(false);
        return R.ok(router.chat(request));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message,
                                   @RequestParam(required = false) String conversationId,
                                   @RequestParam(required = false, defaultValue = "chat") String agentType,
                                   @RequestParam(required = false, defaultValue = "chat") String mode,
                                   @RequestParam(required = false) String tools,
                                   @RequestParam(required = false, defaultValue = "false") boolean thinkingMode,
                                   @RequestParam(required = false, defaultValue = "true") boolean memoryEnabled,
                                   @RequestParam(required = false) Integer topK,
                                   @RequestParam(required = false) Double similarityThreshold,
                                   @RequestParam(required = false, defaultValue = "true") boolean autoExecute,
                                   @RequestParam(required = false, defaultValue = "auto") String strategy) {
        ConversationRunRequest request = new ConversationRunRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        request.setAgentType(agentType);
        request.setMode(mode);
        request.setTools(router.parseTools(tools));
        request.setThinkingMode(thinkingMode);
        request.setMemoryEnabled(memoryEnabled);
        request.setTopK(topK);
        request.setSimilarityThreshold(similarityThreshold);
        request.setAutoExecute(autoExecute);
        request.setStrategy(strategy);
        request.setStream(true);
        return router.stream(request);
    }

    @GetMapping
    public R<?> list(@RequestParam(required = false) String agentType,
                     @RequestParam(required = false) String keyword) {
        return R.ok(historyService.listConversations(learnUserService.currentUserId(), agentType, keyword));
    }

    @GetMapping("/{conversationId}/messages")
    public R<?> messages(@PathVariable String conversationId) {
        return R.ok(historyService.listMessages(learnUserService.currentUserId(), conversationId));
    }

    @PostMapping
    public R<SessionVO> create(@RequestBody(required = false) CreateSessionRequest request) {
        return R.ok(historyService.createConversation(learnUserService.currentUserId(), request == null ? null : request.getTitle()));
    }

    @PatchMapping("/{conversationId}/title")
    public R<SessionVO> rename(@PathVariable String conversationId, @RequestBody UpdateSessionTitleRequest request) {
        return R.ok(historyService.renameConversation(learnUserService.currentUserId(), conversationId, request == null ? null : request.getTitle()));
    }

    @DeleteMapping("/{conversationId}/memory")
    public R<String> clearMemory(@PathVariable String conversationId) {
        historyService.assertOwner(learnUserService.currentUserId(), conversationId);
        memoryService.clearMemory(conversationId);
        return R.ok("会话记忆已清除");
    }

    @DeleteMapping("/{conversationId}")
    public R<String> delete(@PathVariable String conversationId) {
        String userId = learnUserService.currentUserId();
        historyService.assertOwner(userId, conversationId);
        memoryService.clearMemory(conversationId);
        historyService.deleteConversation(userId, conversationId);
        return R.ok("会话已删除");
    }
}
