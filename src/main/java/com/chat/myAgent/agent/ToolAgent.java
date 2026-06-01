package com.chat.myAgent.agent;

import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.model.dto.AgentRequest;
import com.chat.myAgent.model.vo.AgentResponse;
import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.service.AuditService;
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
    private final ChatMemory chatMemory;

    // 注入所有工具
    private final DateTimeTool dateTimeTool;
    private final CalculatorTool calculatorTool;
    private final TranslateTool translateTool;
    private final DocParseTool docParseTool;
    private final DbQueryTool dbQueryTool;
    private final AuditService auditService;
    private final ModelConfig modelConfig;

    @Value("classpath:prompts/tool-agent-system.st")
    private Resource toolAgentSystemPrompt;

    public ToolAgent(
            @Qualifier("baseChatClient") ChatClient baseChatClient,
            ChatMemory chatMemory,
            DateTimeTool dateTimeTool,
            CalculatorTool calculatorTool,
            TranslateTool translateTool,
            DocParseTool docParseTool,
            DbQueryTool dbQueryTool,
            AuditService auditService,
            ModelConfig modelConfig) {
        this.baseChatClient = baseChatClient;
        this.chatMemory = chatMemory;
        this.dateTimeTool = dateTimeTool;
        this.calculatorTool = calculatorTool;
        this.translateTool = translateTool;
        this.docParseTool = docParseTool;
        this.dbQueryTool = dbQueryTool;
        this.auditService = auditService;
        this.modelConfig = modelConfig;
    }

    /**
     * 工具调用对话（无记忆版）
     *
     * 适用场景：单次工具调用，不需要上下文
     */
    public AgentResponse chat(AgentRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());

        log.debug("ToolAgent [{}]: {}", conversationId, request.getMessage());

        String reply = baseChatClient.prompt()
                .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                .user(request.getMessage())
                .tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool)
                .call()
                .content();

        log.debug("ToolAgent [{}] 回复: {}", conversationId, reply);

        AgentResponse response = AgentResponse.builder()
                .conversationId(conversationId)
                .reply(reply)
                .traceId(TraceContext.getTraceId())
                .model(modelConfig.getPrimaryModel())
                .agentType("tool")
                .build();
        auditService.saveAgentInvocation(conversationId, "tool", modelConfig.getPrimaryModel(), request.getMessage(), response.getReply(), null, "SUCCESS", 0L);
        return response;
    }

    /**
     * 工具调用对话（带记忆版）
     *
     * 适用场景：连续的工具调用对话，需要记住之前的交互
     * 例如：
     *   用户: "查一下技术部有哪些人"
     *   用户: "他们的平均薪资是多少" → 需要记住"技术部"这个上下文
     */
    public AgentResponse chatWithMemory(AgentRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());

        log.debug("ToolAgent(Memory) [{}]: {}", conversationId, request.getMessage());

        String reply = baseChatClient.prompt()
                .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                .user(request.getMessage())
                .tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool)
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                )
                .call()
                .content();

        log.debug("ToolAgent(Memory) [{}] 回复: {}", conversationId, reply);

        AgentResponse response = AgentResponse.builder()
                .conversationId(conversationId)
                .reply(reply)
                .traceId(TraceContext.getTraceId())
                .model(modelConfig.getPrimaryModel())
                .agentType("tool-memory")
                .build();
        auditService.saveAgentInvocation(conversationId, "tool-memory", modelConfig.getPrimaryModel(), request.getMessage(), response.getReply(), null, "SUCCESS", 0L);
        return response;
    }

    /**
     * 指定工具对话（只启用部分工具）
     *
     * 适用场景：只想让AI使用特定工具，不暴露其他工具
     * 例如：只启用计算器和时间工具
     */
    public AgentResponse chatWithSpecificTools(AgentRequest request, String... toolNames) {
        String conversationId = resolveConversationId(request.getConversationId());

        log.debug("ToolAgent(Specific) [{}] tools={}: {}", conversationId, toolNames, request.getMessage());

        // 根据工具名称选择要注册的工具
        Object[] selectedTools = selectTools(toolNames);

        String reply = baseChatClient.prompt()
                .system("请直接、简洁、准确地回答用户，不要输出思考过程。")
                .user(request.getMessage())
                .tools(selectedTools)
                .call()
                .content();

        AgentResponse response = AgentResponse.builder()
                .conversationId(conversationId)
                .reply(reply)
                .traceId(TraceContext.getTraceId())
                .model(modelConfig.getPrimaryModel())
                .agentType("tool-specific")
                .build();
        auditService.saveAgentInvocation(conversationId, "tool-specific", modelConfig.getPrimaryModel(), request.getMessage(), response.getReply(), null, "SUCCESS", 0L);
        return response;
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
            }
        }
        return tools.toArray();
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return conversationId;
    }
}
