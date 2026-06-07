package com.chat.myAgent.agent;

import com.chat.myAgent.common.audit.Auditable;
import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.common.stream.StreamEvent;
import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.model.vo.AgentResponse;
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

@Slf4j
@Component
public class FullAgent {

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

    public FullAgent(
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

    public Flux<String> chatStream(String message, String conversationId, String mode, String tools) {
        return chatStream(message, conversationId, mode, tools, true);
    }

    public Flux<String> chatStream(String message, String conversationId, String mode, String tools, boolean memoryEnabled) {
        String resolvedId = resolveConversationId(conversationId);
        String username = getCurrentUsername();
        StringBuilder fullResponse = new StringBuilder();
        try {
            ChatClient.ChatClientRequestSpec prompt = memoryEnabled
                    ? fullAgentClient.prompt()
                    .user(message)
                    .tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, resolvedId))
                    : baseChatClient.prompt()
                    .system(fullAgentPrompt)
                    .user(message)
                    .tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool);
            return prompt
                    .stream()
                    .content()
                    .map(chunk -> {
                        fullResponse.append(chunk);
                        return StreamEvent.delta(chunk).toJson();
                    })
                    .startWith(StreamEvent.start("开始 Agent 流式输出").toJson())
                    .concatWith(Flux.just(StreamEvent.done("完成").toJson()))
                    .doFinally(signalType -> {
                        String status = signalType == reactor.core.publisher.SignalType.ON_COMPLETE ? "SUCCESS" : "FAILED";
                        String agentType = memoryEnabled ? "full-memory" : "full";
                        auditService.saveChatHistory(resolvedId, username, "user", message, agentType, modelConfig.getPrimaryModel(), null, null, 0L);
                        auditService.saveChatHistory(resolvedId, username, "assistant", fullResponse.toString(), agentType, modelConfig.getPrimaryModel(), null, null, 0L);
                        auditService.saveAgentInvocation(resolvedId, agentType, modelConfig.getPrimaryModel(), message, fullResponse.toString(), null, status, 0L);
                    });
        } catch (Exception ex) {
            log.error("FullAgent stream failed", ex);
            return Flux.just(StreamEvent.error("Agent 流式失败: " + ex.getMessage()).toJson());
        }
    }

    public AgentResponse chat(String message, String conversationId) {
        return chat(message, conversationId, true);
    }

    @Auditable(agentType = "full")
    public AgentResponse chat(String message, String conversationId, boolean memoryEnabled) {
        final String resolvedId = resolveConversationId(conversationId);
        try {
            ChatClient.ChatClientRequestSpec prompt = memoryEnabled
                    ? fullAgentClient.prompt().user(message).tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool).advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, resolvedId))
                    : baseChatClient.prompt().system(fullAgentPrompt).user(message).tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool);
            String reply = prompt.call().content();
            String agentType = memoryEnabled ? "full-memory" : "full";
            return AgentResponse.builder().conversationId(resolvedId).reply(reply).model(modelConfig.getPrimaryModel()).agentType(agentType).traceId(TraceContext.getTraceId()).build();
        } catch (Exception ex) {
            log.error("FullAgent chat failed", ex);
            return AgentResponse.builder().conversationId(resolvedId).reply("对话失败: " + ex.getMessage()).model(modelConfig.getPrimaryModel()).agentType("full").traceId(TraceContext.getTraceId()).build();
        }
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName() : "anonymous";
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return "full-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return conversationId;
    }
}
