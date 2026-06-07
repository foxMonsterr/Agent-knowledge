package com.chat.myAgent.conversation.service;

import com.chat.myAgent.agent.*;
import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.conversation.core.ConversationAgentType;
import com.chat.myAgent.conversation.core.ConversationContext;
import com.chat.myAgent.conversation.core.ConversationMode;
import com.chat.myAgent.conversation.dto.ConversationRunRequest;
import com.chat.myAgent.conversation.dto.ConversationRunResponse;
import com.chat.myAgent.conversation.memory.ConversationMemoryService;
import com.chat.myAgent.conversation.stream.ConversationStreamService;
import com.chat.myAgent.learn.dto.ReActChatRequest;
import com.chat.myAgent.learn.service.LearnAgent;
import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.model.dto.AgentRequest;
import com.chat.myAgent.model.dto.ChatRequest;
import com.chat.myAgent.model.vo.AgentResponse;
import com.chat.myAgent.model.vo.ChatResponse;
import com.chat.myAgent.model.vo.KnowledgeResponse;
import com.chat.myAgent.model.vo.PlanningResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConversationAgentRouter {

    private final ChatAgent chatAgent;
    private final ToolAgent toolAgent;
    private final StreamAgent streamAgent;
    private final RagAgent ragAgent;
    private final PlanningAgent planningAgent;
    private final FullAgent fullAgent;
    private final LearnAgent learnAgent;
    private final GeneralReActAgent generalReActAgent;
    private final LearnUserService learnUserService;
    private final ConversationMemoryService memoryService;
    private final ConversationStreamService streamService;

    public ConversationRunResponse chat(ConversationRunRequest request) {
        ConversationContext context = context(request, false);
        long startedAt = System.currentTimeMillis();
        try {
            return switch (context.getAgentType()) {
                case CHAT -> chatWithChatAgent(context, request, startedAt);
                case TOOL -> chatWithToolAgent(context, request, startedAt);
                case RAG -> chatWithRagAgent(context, request, startedAt);
                case PLANNING -> chatWithPlanningAgent(context, request, startedAt);
                case FULL -> chatWithFullAgent(context, request, startedAt);
                case LEARN_REACT -> chatWithLearnReAct(context, request, startedAt);
                case GENERAL_REACT -> chatWithGeneralReAct(context, request, startedAt);
            };
        } catch (Exception e) {
            return ConversationRunResponse.builder()
                    .conversationId(context.getConversationId())
                    .traceId(TraceContext.getTraceId())
                    .agentType(context.agentTypeValue())
                    .mode(context.modeValue())
                    .reply("对话失败: " + safeMessage(e))
                    .memoryEnabled(context.isMemoryEnabled())
                    .status("failed")
                    .totalTimeMs(System.currentTimeMillis() - startedAt)
                    .build();
        }
    }

    public Flux<String> stream(ConversationRunRequest request) {
        ConversationContext context = context(request, true);
        Flux<String> raw = switch (context.getAgentType()) {
            case CHAT -> streamAgent.streamChat(context.getMessage(), context.getConversationId(), context.isMemoryEnabled());
            case TOOL -> {
                if (context.isThinkingMode()) {
                    yield generalReActAgent.stream(context.getUserId(), toAgentRequest(context, request), context.modeValue());
                }
                yield streamAgent.streamChatWithTools(context.getMessage(), context.getConversationId(), context.isMemoryEnabled());
            }
            case RAG -> ragAgent.askStream(context.getMessage(), context.getConversationId(),
                    ConversationMode.RAG_MANUAL.equals(context.getMode()), context.isMemoryEnabled());
            case PLANNING -> planningAgent.planStream(context.getMessage(), context.getConversationId(),
                    request.getAutoExecute() == null || request.getAutoExecute(), context.modeValue(), context.isMemoryEnabled());
            case FULL -> fullAgent.chatStream(context.getMessage(), context.getConversationId(), context.modeValue(),
                    request.getTools() == null ? "" : String.join(",", request.getTools()), context.isMemoryEnabled());
            case LEARN_REACT -> learnAgent.stream(context.getUserId(), toReActRequest(context, request));
            case GENERAL_REACT -> generalReActAgent.stream(context.getUserId(), toAgentRequest(context, request), context.modeValue());
        };
        return streamService.wrapRawEvents(raw, context);
    }

    public ConversationContext context(ConversationRunRequest request, boolean stream) {
        String conversationId = memoryService.resolveConversationId(request.getConversationId());
        ConversationAgentType agentType = ConversationAgentType.from(request.getAgentType());
        ConversationMode mode = ConversationMode.from(request.getMode());
        boolean thinkingMode = Boolean.TRUE.equals(request.getThinkingMode());
        if (thinkingMode && (ConversationAgentType.CHAT.equals(agentType) || ConversationAgentType.TOOL.equals(agentType))) {
            agentType = ConversationAgentType.GENERAL_REACT;
            mode = ConversationMode.REACT;
        }
        String userId = learnUserService.currentUserId();
        return ConversationContext.builder()
                .userId(userId)
                .username(userId)
                .conversationId(conversationId)
                .traceId(TraceContext.getTraceId())
                .agentType(agentType)
                .mode(mode)
                .message(request.getMessage())
                .memoryEnabled(memoryService.isMemoryEnabled(request.getMemoryEnabled()))
                .stream(stream)
                .thinkingMode(thinkingMode)
                .tools(request.getTools() == null ? List.of() : request.getTools())
                .metadata(request.getMetadata() == null ? new LinkedHashMap<>() : request.getMetadata())
                .startedAt(LocalDateTime.now())
                .build();
    }

    private ConversationRunResponse chatWithChatAgent(ConversationContext context, ConversationRunRequest request, long startedAt) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setConversationId(context.getConversationId());
        chatRequest.setMessage(context.getMessage());
        chatRequest.setMemoryEnabled(context.isMemoryEnabled());
        chatRequest.setThinkingMode(context.isThinkingMode());
        chatRequest.setModel(request.getModel());
        chatRequest.setRole(request.getRole());
        chatRequest.setLevel(request.getLevel());
        ChatResponse response = chatAgent.chat(chatRequest);
        return baseResponse(context, response.getReply(), response.getModel(), response.getTraceId(), startedAt)
                .build();
    }

    private ConversationRunResponse chatWithToolAgent(ConversationContext context, ConversationRunRequest request, long startedAt) {
        AgentRequest agentRequest = toAgentRequest(context, request);
        AgentResponse response;
        if (ConversationMode.SPECIFIC.equals(context.getMode()) && !context.getTools().isEmpty()) {
            response = toolAgent.chatWithSpecificTools(agentRequest, context.getTools().toArray(new String[0]));
        } else {
            response = toolAgent.chat(agentRequest);
        }
        return baseResponse(context, response.getReply(), response.getModel(), response.getTraceId(), startedAt)
                .thinking(response.getThinking())
                .build();
    }

    private ConversationRunResponse chatWithRagAgent(ConversationContext context, ConversationRunRequest request, long startedAt) {
        KnowledgeResponse response = ConversationMode.RAG_MANUAL.equals(context.getMode())
                ? ragAgent.askManual(context.getMessage(), context.getConversationId(), context.isMemoryEnabled())
                : ragAgent.ask(context.getMessage(), context.getConversationId(), context.isMemoryEnabled());
        return baseResponse(context, response.getAnswer(), response.getModel(), response.getTraceId(), startedAt)
                .sources(response.getSources())
                .retrievedChunks(response.getRetrievedChunks())
                .build();
    }

    private ConversationRunResponse chatWithPlanningAgent(ConversationContext context, ConversationRunRequest request, long startedAt) {
        boolean autoExecute = !ConversationMode.PLANNING_ONLY.equals(context.getMode())
                && (request.getAutoExecute() == null || request.getAutoExecute());
        PlanningResponse response = planningAgent.planAndExecute(context.getMessage(), context.getConversationId(),
                autoExecute, context.isMemoryEnabled());
        String reply = response.getFinalAnswer() != null ? response.getFinalAnswer() : response.getDirectAnswer();
        return baseResponse(context, reply, null, response.getTraceId(), startedAt)
                .planned(response.isPlanned())
                .steps(response.getSteps())
                .totalTimeMs(response.getTotalTimeMs())
                .build();
    }

    private ConversationRunResponse chatWithFullAgent(ConversationContext context, ConversationRunRequest request, long startedAt) {
        AgentResponse response = fullAgent.chat(context.getMessage(), context.getConversationId(), context.isMemoryEnabled());
        return baseResponse(context, response.getReply(), response.getModel(), response.getTraceId(), startedAt)
                .thinking(response.getThinking())
                .build();
    }

    private ConversationRunResponse chatWithLearnReAct(ConversationContext context, ConversationRunRequest request, long startedAt) {
        Map<String, Object> response = learnAgent.chat(context.getUserId(), toReActRequest(context, request));
        return baseResponse(context, string(response.get("reply")), string(response.get("model")), string(response.get("traceId")), startedAt)
                .metadata(response)
                .build();
    }

    private ConversationRunResponse chatWithGeneralReAct(ConversationContext context, ConversationRunRequest request, long startedAt) {
        AgentResponse response = generalReActAgent.chat(context.getUserId(), toAgentRequest(context, request), context.modeValue());
        return baseResponse(context, response.getReply(), response.getModel(), response.getTraceId(), startedAt)
                .thinking(response.getThinking())
                .build();
    }

    private ConversationRunResponse.ConversationRunResponseBuilder baseResponse(ConversationContext context,
                                                                                 String reply,
                                                                                 String model,
                                                                                 String traceId,
                                                                                 long startedAt) {
        return ConversationRunResponse.builder()
                .conversationId(context.getConversationId())
                .traceId(traceId == null ? context.getTraceId() : traceId)
                .agentType(context.agentTypeValue())
                .mode(context.modeValue())
                .reply(reply)
                .model(model)
                .memoryEnabled(context.isMemoryEnabled())
                .status("success")
                .totalTimeMs(System.currentTimeMillis() - startedAt);
    }

    private AgentRequest toAgentRequest(ConversationContext context, ConversationRunRequest request) {
        AgentRequest agentRequest = new AgentRequest();
        agentRequest.setConversationId(context.getConversationId());
        agentRequest.setMessage(context.getMessage());
        agentRequest.setThinkingMode(context.isThinkingMode());
        agentRequest.setMemoryEnabled(context.isMemoryEnabled());
        agentRequest.setTools(request.getTools());
        return agentRequest;
    }

    private ReActChatRequest toReActRequest(ConversationContext context, ConversationRunRequest request) {
        ReActChatRequest reactRequest = new ReActChatRequest();
        reactRequest.setConversationId(context.getConversationId());
        reactRequest.setSessionId(context.getConversationId());
        reactRequest.setMessage(context.getMessage());
        reactRequest.setStrategy(request.getStrategy());
        reactRequest.setNoteIds(request.getNoteIds());
        reactRequest.setTags(request.getTags());
        reactRequest.setCategory(request.getCategory());
        reactRequest.setStream(request.getStream());
        reactRequest.setMaxIterations(request.getMaxIterations());
        reactRequest.setAutoCreateNote(request.getAutoCreateNote());
        reactRequest.setMemoryEnabled(context.isMemoryEnabled());
        return reactRequest;
    }

    public List<String> parseTools(String tools) {
        if (tools == null || tools.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tools.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
