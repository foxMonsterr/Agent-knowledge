package com.chat.myAgent.service;

import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.model.entity.AgentInvocationEntity;
import com.chat.myAgent.model.entity.ChatHistoryEntity;
import com.chat.myAgent.repository.AgentInvocationRepository;
import com.chat.myAgent.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计服务
 *
 * 统一保存聊天历史和 Agent 调用记录，便于后续统计、监控和回放。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final AgentInvocationRepository agentInvocationRepository;

    public void saveChatHistory(String conversationId,
                                String username,
                                String role,
                                String content,
                                String agentType,
                                String model,
                                Long promptTokens,
                                Long completionTokens,
                                Long latencyMs) {
        ChatHistoryEntity entity = new ChatHistoryEntity();
        entity.setConversationId(conversationId);
        entity.setUsername(username != null ? username : "anonymous");
        entity.setMessageRole(role);
        entity.setContent(content);
        entity.setAgentType(agentType);
        entity.setModel(model);
        entity.setPromptTokens(promptTokens);
        entity.setCompletionTokens(completionTokens);
        entity.setLatencyMs(latencyMs);
        chatHistoryRepository.save(entity);
        log.debug("已保存聊天历史, traceId={}, conversationId={}", TraceContext.getTraceId(), conversationId);
    }

    public void saveAgentInvocation(String conversationId,
                                    String agentType,
                                    String model,
                                    String inputText,
                                    String outputText,
                                    String thinkingText,
                                    String status,
                                    Long latencyMs) {
        AgentInvocationEntity entity = new AgentInvocationEntity();
        entity.setInvocationId(TraceContext.getTraceId());
        entity.setConversationId(conversationId);
        entity.setAgentType(agentType);
        entity.setModel(model);
        entity.setTraceId(TraceContext.getTraceId());
        entity.setInputText(inputText);
        entity.setOutputText(outputText);
        entity.setThinkingText(thinkingText);
        entity.setStatus(status);
        entity.setLatencyMs(latencyMs);
        agentInvocationRepository.save(entity);
        log.debug("已保存 Agent 调用记录, traceId={}, agentType={}", TraceContext.getTraceId(), agentType);
    }

    public long countAgentRunsByType(String agentType) {
        return agentInvocationRepository.countByAgentType(agentType);
    }

    public long countChatHistoryByUsername(String username) {
        return chatHistoryRepository.countByUsername(username);
    }

    public Long sumTotalTokens() {
        return chatHistoryRepository.sumTotalTokens();
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
