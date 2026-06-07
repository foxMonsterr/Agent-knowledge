package com.chat.myAgent.agent;

import com.chat.myAgent.common.audit.Auditable;
import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.model.dto.AgentRequest;
import com.chat.myAgent.model.vo.AgentResponse;
import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 工具调用 Agent
 *
 * 核心能力：
 * 1. 大模型自主决定是否调用工具
 * 2. 自动执行工具并将结果回传给模型
 * 3. 模型整合工具结果后生成最终回复
 * 4. 同时具备多轮记忆（带上下文的工具调用）
 *
 * 工具调用流程：
 * 用户消息 → 模型分析 → [决定调用工具] → 执行工具 → 结果回传 → 模型生成最终回答
 *                      → [不需要工具] → 直接回答
 */
@Slf4j
@Component
public class ToolAgent {

    private final ChatClient baseChatClient;
    private final ChatClient toolChatClient;
    private final ChatMemory chatMemory;

    // 注入所有工具
    private final DateTimeTool dateTimeTool;
    private final CalculatorTool calculatorTool;
    private final TranslateTool translateTool;
    private final DocParseTool docParseTool;
    private final DbQueryTool dbQueryTool;
    private final TextTool textTool;
    private final JsonTool jsonTool;
    private final CollectionTool collectionTool;
    private final RegexTool regexTool;
    private final SystemInfoTool systemInfoTool;
    private final ModelConfig modelConfig;

    @Value("classpath:prompts/tool-agent-system.st")
    private Resource toolAgentSystemPrompt;

    public ToolAgent(
            @Qualifier("baseChatClient") ChatClient baseChatClient,
            @Qualifier("toolChatClient") ChatClient toolChatClient,
            ChatMemory chatMemory,
            DateTimeTool dateTimeTool,
            CalculatorTool calculatorTool,
            TranslateTool translateTool,
            DocParseTool docParseTool,
            DbQueryTool dbQueryTool,
            TextTool textTool,
            JsonTool jsonTool,
            CollectionTool collectionTool,
            RegexTool regexTool,
            SystemInfoTool systemInfoTool,
            ModelConfig modelConfig) {
        this.baseChatClient = baseChatClient;
        this.toolChatClient = toolChatClient;
        this.chatMemory = chatMemory;
        this.dateTimeTool = dateTimeTool;
        this.calculatorTool = calculatorTool;
        this.translateTool = translateTool;
        this.docParseTool = docParseTool;
        this.dbQueryTool = dbQueryTool;
        this.textTool = textTool;
        this.jsonTool = jsonTool;
        this.collectionTool = collectionTool;
        this.regexTool = regexTool;
        this.systemInfoTool = systemInfoTool;
        this.modelConfig = modelConfig;
    }

    /**
     * 工具调用对话（无记忆版）
     *
     * 适用场景：单次工具调用，不需要上下文
     */
    @Auditable(agentType = "tool")
    public AgentResponse chat(AgentRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        boolean memoryEnabled = memoryEnabled(request.getMemoryEnabled());

        log.debug("ToolAgent [{}]: {}", conversationId, request.getMessage());

        ChatClient client = memoryEnabled ? toolChatClient : baseChatClient;
        String reply = client.prompt()
                .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                .user(request.getMessage())
                .tools(allTools())
                .advisors(advisor -> {
                    if (memoryEnabled) {
                        advisor.param(ChatMemory.CONVERSATION_ID, conversationId);
                    }
                })
                .call()
                .content();

        log.debug("ToolAgent [{}] 回复: {}", conversationId, reply);

        return AgentResponse.builder()
                .conversationId(conversationId)
                .reply(reply)
                .traceId(TraceContext.getTraceId())
                .model(modelConfig.getPrimaryModel())
                .agentType(memoryEnabled ? "tool-memory" : "tool")
                .build();
    }

    /**
     * 工具调用对话（带记忆版）
     *
     * 适用场景：连续的工具调用对话，需要记住之前的交互
     * 例如：
     *   用户: "查一下技术部有哪些人"
     *   用户: "他们的平均薪资是多少" → 需要记住"技术部"这个上下文
     */
    @Auditable(agentType = "tool-memory")
    public AgentResponse chatWithMemory(AgentRequest request) {
        request.setMemoryEnabled(true);
        String conversationId = resolveConversationId(request.getConversationId());

        log.debug("ToolAgent(Memory) [{}]: {}", conversationId, request.getMessage());

        String reply = toolChatClient.prompt()
                .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                .user(request.getMessage())
                .tools(allTools())
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                )
                .call()
                .content();

        log.debug("ToolAgent(Memory) [{}] 回复: {}", conversationId, reply);

        return AgentResponse.builder()
                .conversationId(conversationId)
                .reply(reply)
                .traceId(TraceContext.getTraceId())
                .model(modelConfig.getPrimaryModel())
                .agentType("tool-memory")
                .build();
    }

    /**
     * 指定工具对话（只启用部分工具）
     *
     * 适用场景：只想让AI使用特定工具，不暴露其他工具
     * 例如：只启用计算器和时间工具
     */
    @Auditable(agentType = "tool-specific")
    public AgentResponse chatWithSpecificTools(AgentRequest request, String... toolNames) {
        String conversationId = resolveConversationId(request.getConversationId());
        boolean memoryEnabled = memoryEnabled(request.getMemoryEnabled());

        log.debug("ToolAgent(Specific) [{}] tools={}: {}", conversationId, toolNames, request.getMessage());

        Object[] selectedTools = selectTools(toolNames);

        ChatClient client = memoryEnabled ? toolChatClient : baseChatClient;
        String reply = client.prompt()
                .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                .user(request.getMessage())
                .tools(selectedTools)
                .advisors(advisor -> {
                    if (memoryEnabled) {
                        advisor.param(ChatMemory.CONVERSATION_ID, conversationId);
                    }
                })
                .call()
                .content();

        return AgentResponse.builder()
                .conversationId(conversationId)
                .reply(reply)
                .traceId(TraceContext.getTraceId())
                .model(modelConfig.getPrimaryModel())
                .agentType(memoryEnabled ? "tool-specific-memory" : "tool-specific")
                .build();
    }

    /**
     * 根据工具名称选择工具实例
     */
    private Object[] selectTools(String... toolNames) {
        List<Object> tools = new java.util.ArrayList<>();
        for (String name : toolNames) {
            switch (name.toLowerCase()) {
                case "datetime", "time" -> tools.add(dateTimeTool);
                case "calculator", "calc" -> tools.add(calculatorTool);
                case "translate" -> tools.add(translateTool);
                case "doc", "document" -> tools.add(docParseTool);
                case "db", "database" -> tools.add(dbQueryTool);
                case "text" -> tools.add(textTool);
                case "json" -> tools.add(jsonTool);
                case "collection", "list" -> tools.add(collectionTool);
                case "regex" -> tools.add(regexTool);
                case "system", "capability" -> tools.add(systemInfoTool);
            }
        }
        return tools.toArray();
    }

    private Object[] allTools() {
        return new Object[]{
                dateTimeTool,
                calculatorTool,
                translateTool,
                docParseTool,
                dbQueryTool,
                textTool,
                jsonTool,
                collectionTool,
                regexTool,
                systemInfoTool
        };
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return conversationId;
    }

    private boolean memoryEnabled(Boolean value) {
        return value == null || value;
    }

}
