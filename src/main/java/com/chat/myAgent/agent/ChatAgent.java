package com.chat.myAgent.agent;

import com.chat.myAgent.common.audit.Auditable;
import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.common.stream.StreamEvent;
import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.model.dto.ChatRequest;
import com.chat.myAgent.model.vo.ChatResponse;
import com.chat.myAgent.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ChatAgent {

    private final ChatClient baseChatClient;
    private final ChatClient memoryChatClient;
    private final ChatClient fallbackChatClient;
    private final ChatMemory chatMemory;
    private final ModelConfig modelConfig;
    private final AuditService auditService;

    @Value("classpath:prompts/expert-system.st")
    private Resource expertPromptResource;

    public ChatAgent(
            @Qualifier("baseChatClient") ChatClient baseChatClient,
            @Qualifier("memoryChatClient") ChatClient memoryChatClient,
            @Qualifier("fallbackChatClient") ChatClient fallbackChatClient,
            ChatMemory chatMemory,
            ModelConfig modelConfig,
            AuditService auditService) {
        this.baseChatClient = baseChatClient;
        this.memoryChatClient = memoryChatClient;
        this.fallbackChatClient = fallbackChatClient;
        this.chatMemory = chatMemory;
        this.modelConfig = modelConfig;
        this.auditService = auditService;
    }

    public Flux<String> memoryChatStream(ChatRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        String userMessage = request.getMessage();
        boolean memoryEnabled = memoryEnabled(request.getMemoryEnabled());
        StringBuilder replyBuffer = new StringBuilder();

        try {
            Flux<String> contentFlux = memoryEnabled
                    ? memoryChatClient.prompt()
                    .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                    .user(userMessage)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    : baseChatClient.prompt()
                    .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                    .user(userMessage)
                    .stream()
                    .content();

            String username = getCurrentUsername();

            return Flux.concat(
                    Flux.just(StreamEvent.start("开始流式记忆对话").toJson()),
                    contentFlux
                            .filter(chunk -> chunk != null && !chunk.isEmpty())
                            .doOnNext(replyBuffer::append)
                            .map(chunk -> StreamEvent.delta(chunk).toJson())
                            .concatWith(Flux.just(StreamEvent.done("完成").toJson()))
                            .doFinally(signalType -> {
                                String reply = replyBuffer.toString();
                                String agentType = memoryEnabled ? "memory" : "chat";
                                auditService.saveChatHistory(conversationId, username, "user", userMessage, agentType, modelConfig.getPrimaryModel(), null, null, 0L);
                                if (!reply.isBlank()) {
                                    auditService.saveChatHistory(conversationId, username, "assistant", reply, agentType, modelConfig.getPrimaryModel(), null, null, 0L);
                                    auditService.saveAgentInvocation(conversationId, agentType + "-stream", modelConfig.getPrimaryModel(), userMessage, reply, null, "SUCCESS", 0L);
                                }
                            })
            );
        } catch (Exception ex) {
            return Flux.just(StreamEvent.error(ex.getMessage() == null ? "流式对话失败" : ex.getMessage()).toJson());
        }
    }

    @Auditable(agentType = "chat")
    public ChatResponse chat(ChatRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        String userMessage = request.getMessage();
        boolean memoryEnabled = memoryEnabled(request.getMemoryEnabled());
        try {
            String reply = memoryEnabled
                    ? memoryChatClient.prompt()
                    .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                    .user(userMessage)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content()
                    : baseChatClient.prompt()
                    .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                    .user(userMessage)
                    .call()
                    .content();
            int historySize = getHistorySize(conversationId);
            return buildResponse(conversationId, reply, modelConfig.getPrimaryModel(), historySize);
        } catch (Exception primaryEx) {
            return fallbackMemoryChat(conversationId, userMessage, memoryEnabled, primaryEx);
        }
    }

    @Auditable(agentType = "expert")
    public ChatResponse expertChat(ChatRequest request, String role, String level) {
        String conversationId = resolveConversationId(request.getConversationId());
        String userMessage = request.getMessage();
        try {
            String reply = baseChatClient.prompt()
                    .system(s -> s.text(expertPromptResource).param("role", role).param("level", level))
                    .user(userMessage)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            int historySize = getHistorySize(conversationId);
            return buildResponse(conversationId, reply, modelConfig.getPrimaryModel(), historySize);
        } catch (Exception primaryEx) {
            return fallbackExpertChat(conversationId, userMessage, role, level, primaryEx);
        }
    }

    public List<Message> getHistory(String conversationId) { return chatMemory.get(conversationId); }
    public void clearMemory(String conversationId) { chatMemory.clear(conversationId); }

    private ChatResponse fallbackMemoryChat(String conversationId, String userMessage, boolean memoryEnabled, Exception primaryEx) {
        try {
            String reply = memoryEnabled
                    ? fallbackChatClient.prompt()
                    .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                    .user(userMessage)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content()
                    : fallbackChatClient.prompt()
                    .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                    .user(userMessage)
                    .call()
                    .content();
            int historySize = getHistorySize(conversationId);
            ChatResponse response = buildResponse(conversationId, reply, modelConfig.getFallbackModelName(), historySize);
            auditService.saveAgentInvocation(conversationId, "memory-fallback", modelConfig.getFallbackModelName(), userMessage, response.getReply(), null, "SUCCESS", 0L);
            return response;
        } catch (Exception fallbackEx) {
            throw new RuntimeException("主模型与兜底模型均调用失败: " + fallbackEx.getMessage(), fallbackEx);
        }
    }

    private ChatResponse fallbackExpertChat(String conversationId, String userMessage, String role, String level, Exception primaryEx) {
        try {
            String reply = fallbackChatClient.prompt()
                    .system(s -> s.text(expertPromptResource).param("role", role).param("level", level))
                    .user(userMessage)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            int historySize = getHistorySize(conversationId);
            ChatResponse response = buildResponse(conversationId, reply, modelConfig.getFallbackModelName(), historySize);
            auditService.saveAgentInvocation(conversationId, "expert-fallback", modelConfig.getFallbackModelName(), userMessage, response.getReply(), null, "SUCCESS", 0L);
            return response;
        } catch (Exception fallbackEx) {
            throw new RuntimeException("主模型与兜底模型均调用失败: " + fallbackEx.getMessage(), fallbackEx);
        }
    }

    private ChatResponse buildResponse(String conversationId, String reply, String modelName, Integer historySize) {
        return ChatResponse.builder()
                .conversationId(conversationId)
                .reply(reply)
                .traceId(TraceContext.getTraceId())
                .model(modelName)
                .historySize(historySize)
                .build();
    }

    private int getHistorySize(String conversationId) {
        List<Message> messages = chatMemory.get(conversationId);
        return messages != null ? messages.size() : 0;
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName() : "anonymous";
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return UUID.randomUUID().toString().replace("-", "");
        return conversationId;
    }

    private boolean memoryEnabled(Boolean value) {
        return value == null || value;
    }

}
