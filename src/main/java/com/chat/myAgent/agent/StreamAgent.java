package com.chat.myAgent.agent;

import com.chat.myAgent.common.stream.StreamEvent;
import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.service.AuditService;
import com.chat.myAgent.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 流式对话 Agent
 *
 * 核心能力：通过 SSE（Server-Sent Events）实时推送AI生成的内容
 * 
 * 与普通对话的区别：
 * - 普通对话：等AI全部生成完毕后一次性返回 → 用户等待时间长
 * - 流式对话：AI边生成边推送 → 用户立即看到内容，体验更好
 *
 * Spring AI 通过 .stream() 方法返回 Flux<String>，天然支持流式
 */
@Slf4j
@Component
public class StreamAgent {

    private final ChatClient fullAgentClient;
    private final ChatClient baseChatClient;
    private final ModelConfig modelConfig;
    private final AuditService auditService;

    private final DateTimeTool dateTimeTool;
    private final CalculatorTool calculatorTool;
    private final TranslateTool translateTool;
    private final DocParseTool docParseTool;
    private final DbQueryTool dbQueryTool;

    @Value("classpath:prompts/full-agent-system.st")
    private Resource fullAgentPrompt;

    public StreamAgent(
            @Qualifier("fullAgentClient") ChatClient fullAgentClient,
            @Qualifier("baseChatClient") ChatClient baseChatClient,
            ModelConfig modelConfig,
            AuditService auditService,
            DateTimeTool dateTimeTool,
            CalculatorTool calculatorTool,
            TranslateTool translateTool,
            DocParseTool docParseTool,
            DbQueryTool dbQueryTool) {
        this.fullAgentClient = fullAgentClient;
        this.baseChatClient = baseChatClient;
        this.modelConfig = modelConfig;
        this.auditService = auditService;
        this.dateTimeTool = dateTimeTool;
        this.calculatorTool = calculatorTool;
        this.translateTool = translateTool;
        this.docParseTool = docParseTool;
        this.dbQueryTool = dbQueryTool;
    }

    public Flux<String> streamChat(String message, String conversationId) {
        return streamChat(message, conversationId, true);
    }

    public Flux<String> streamChat(String message, String conversationId, boolean memoryEnabled) {
        return doStream(message, conversationId, memoryEnabled, false);
    }

    public Flux<String> streamChatWithTools(String message, String conversationId) {
        return streamChatWithTools(message, conversationId, true);
    }

    public Flux<String> streamChatWithTools(String message, String conversationId, boolean memoryEnabled) {
        return doStream(message, conversationId, memoryEnabled, true);
    }

    private Flux<String> doStream(String message, String conversationId, boolean memoryEnabled, boolean withTools) {
        final String resolvedId = resolveConversationId(conversationId);
        String label = withTools ? "stream-tools" : "stream";
        String startMsg = withTools ? "开始工具流式对话" : "开始流式对话";
        log.debug("StreamAgent [{}] 流式对话: {}", resolvedId, message);
        String username = getCurrentUsername();
        StringBuilder fullResponse = new StringBuilder();
        try {
            ChatClient.ChatClientRequestSpec prompt;
            if (memoryEnabled) {
                prompt = fullAgentClient.prompt().user(message)
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, resolvedId));
            } else {
                prompt = baseChatClient.prompt().user(message);
                if (withTools) {
                    prompt = prompt.system(fullAgentPrompt);
                } else {
                    prompt = prompt.system("请直接、简洁、准确地回答用户，不要输出思考过程。");
                }
            }
            if (withTools) {
                prompt = prompt.tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool);
            }
            Flux<String> content = prompt.stream().content();

            String agentType = memoryEnabled ? label + "-memory" : label;
            return Flux.concat(
                    Flux.just(StreamEvent.start(startMsg).toJson()),
                    content.map(chunk -> {
                        fullResponse.append(chunk);
                        return StreamEvent.delta(chunk).toJson();
                    }),
                    Flux.just(StreamEvent.done("完成").toJson())
            ).doFinally(signalType -> {
                String status = signalType == reactor.core.publisher.SignalType.ON_COMPLETE ? "SUCCESS" : "FAILED";
                auditService.saveChatHistory(resolvedId, username, "user", message, agentType, modelConfig.getPrimaryModel(), null, null, 0L);
                auditService.saveChatHistory(resolvedId, username, "assistant", fullResponse.toString(), agentType, modelConfig.getPrimaryModel(), null, null, 0L);
                auditService.saveAgentInvocation(resolvedId, agentType, modelConfig.getPrimaryModel(), message, fullResponse.toString(), null, status, 0L);
            });
        } catch (Exception ex) {
            log.error("StreamAgent doStream({}) failed", label, ex);
            return Flux.just(StreamEvent.error("流式对话失败: " + ex.getMessage()).toJson());
        }
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName() : "anonymous";
    }

    /**
     * 会话ID 为空时自动生成
     */
    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "stream-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        return conversationId;
    }
}
