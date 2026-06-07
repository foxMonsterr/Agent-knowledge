package com.chat.myAgent.agent.planning;

import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.common.stream.StreamEvent;
import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.model.vo.PlanningResponse;
import com.chat.myAgent.model.vo.StepResult;
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

import java.util.List;

@Slf4j
@Component
public class PlanningResponseBuilder {

    private final ChatClient baseChatClient;
    private final ChatClient fullAgentClient;
    private final DateTimeTool dateTimeTool;
    private final CalculatorTool calculatorTool;
    private final TranslateTool translateTool;
    private final DocParseTool docParseTool;
    private final DbQueryTool dbQueryTool;
    private final AuditService auditService;
    private final ModelConfig modelConfig;

    @Value("classpath:prompts/full-agent-system.st")
    private Resource fullAgentPrompt;

    public PlanningResponseBuilder(
            @Qualifier("baseChatClient") ChatClient baseChatClient,
            @Qualifier("fullAgentClient") ChatClient fullAgentClient,
            DateTimeTool dateTimeTool,
            CalculatorTool calculatorTool,
            TranslateTool translateTool,
            DocParseTool docParseTool,
            DbQueryTool dbQueryTool,
            AuditService auditService,
            ModelConfig modelConfig) {
        this.baseChatClient = baseChatClient;
        this.fullAgentClient = fullAgentClient;
        this.dateTimeTool = dateTimeTool;
        this.calculatorTool = calculatorTool;
        this.translateTool = translateTool;
        this.docParseTool = docParseTool;
        this.dbQueryTool = dbQueryTool;
        this.auditService = auditService;
        this.modelConfig = modelConfig;
    }

    public String generateFinalAnswer(String originalTask, List<StepResult> steps) {
        return baseChatClient.prompt().user(buildFinalAnswerPrompt(originalTask, steps)).call().content();
    }

    public String buildFinalAnswerPrompt(String originalTask, List<StepResult> steps) {
        StringBuilder context = new StringBuilder();
        context.append("用户原始需求：").append(originalTask).append("\n\n");
        context.append("各步骤执行结果：\n");
        for (StepResult step : steps) {
            context.append("步骤").append(step.getStepNumber()).append(": ").append(step.getDescription()).append("\n");
            context.append("结果: ").append(step.getResult()).append("\n");
            context.append("状态: ").append(step.isSuccess() ? "成功" : "失败").append("\n\n");
        }
        context.append("请基于以上所有步骤的执行结果，生成一个完整、连贯的最终回答给用户。");
        return context.toString();
    }

    public static String cleanJsonResponse(String json) {
        if (json == null) return "{}";
        json = json.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        return json.trim();
    }

    public Flux<String> streamDirectAnswer(String task, String conversationId, boolean memoryEnabled, long startTime) {
        StringBuilder fullResponse = new StringBuilder();
        ChatClient.ChatClientRequestSpec prompt = memoryEnabled
                ? fullAgentClient.prompt()
                .user(task)
                .tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                : baseChatClient.prompt()
                .system(fullAgentPrompt)
                .user(task)
                .tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool);

        return Flux.concat(
                Flux.just(StreamEvent.start("开始直接回答").toJson()),
                prompt.stream().content().map(chunk -> {
                    fullResponse.append(chunk);
                    return StreamEvent.delta(chunk).toJson();
                }),
                Flux.just(StreamEvent.done("完成").toJson())
        ).doFinally(signalType -> saveStreamHistory(
                conversationId,
                task,
                fullResponse.toString(),
                "planning-stream-direct",
                startTime,
                signalType == reactor.core.publisher.SignalType.ON_COMPLETE ? "SUCCESS" : "FAILED"
        ));
    }

    public PlanningResponse fallbackDirectExecution(String task, String conversationId, long startTime, boolean memoryEnabled) {
        String reply = memoryEnabled
                ? fullAgentClient.prompt().user(task).tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool).advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId)).call().content()
                : baseChatClient.prompt().user(task).tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool).call().content();
        long totalTime = System.currentTimeMillis() - startTime;
        PlanningResponse response = PlanningResponse.builder()
                .conversationId(conversationId)
                .planned(false)
                .directAnswer(reply)
                .finalAnswer(reply)
                .totalTimeMs(totalTime)
                .traceId(TraceContext.getTraceId())
                .build();
        saveChatHistory(conversationId, task, response.getFinalAnswer(), "planning-fallback", totalTime);
        auditService.saveAgentInvocation(conversationId, "planning-fallback", modelConfig.getPrimaryModel(), task, response.getFinalAnswer(), null, "SUCCESS", totalTime);
        return response;
    }

    private void saveStreamHistory(String conversationId, String task, String reply, String agentType, long startTime, String status) {
        long totalTime = System.currentTimeMillis() - startTime;
        String username = getCurrentUsername();
        auditService.saveChatHistory(conversationId, username, "user", task, agentType, modelConfig.getPrimaryModel(), null, null, 0L);
        auditService.saveChatHistory(conversationId, username, "assistant", reply == null ? "" : reply, agentType, modelConfig.getPrimaryModel(), null, null, totalTime);
        auditService.saveAgentInvocation(conversationId, agentType, modelConfig.getPrimaryModel(), task, reply, null, status, totalTime);
    }

    private void saveChatHistory(String conversationId, String task, String reply, String agentType, long totalTime) {
        String username = getCurrentUsername();
        auditService.saveChatHistory(conversationId, username, "user", task, agentType, modelConfig.getPrimaryModel(), null, null, 0L);
        auditService.saveChatHistory(conversationId, username, "assistant", reply, agentType, modelConfig.getPrimaryModel(), null, null, totalTime);
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName() : "anonymous";
    }
}
