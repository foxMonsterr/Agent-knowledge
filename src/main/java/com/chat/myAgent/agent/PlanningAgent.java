package com.chat.myAgent.agent;

import com.chat.myAgent.agent.planning.PlanningResponseBuilder;
import com.chat.myAgent.agent.planning.PlanningStepExecutor;
import com.chat.myAgent.common.audit.Auditable;
import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.common.stream.StreamEvent;
import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.model.vo.PlanningResponse;
import com.chat.myAgent.model.vo.StepResult;
import com.chat.myAgent.service.AuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class PlanningAgent {

    private final ChatClient baseChatClient;
    private final ChatClient memoryChatClient;
    private final ObjectMapper objectMapper;
    private final ModelConfig modelConfig;
    private final AuditService auditService;
    private final PlanningStepExecutor stepExecutor;
    private final PlanningResponseBuilder responseBuilder;

    @Value("classpath:prompts/planning-system.st")
    private Resource planningSystemPrompt;

    public PlanningAgent(
            @Qualifier("baseChatClient") ChatClient baseChatClient,
            @Qualifier("memoryChatClient") ChatClient memoryChatClient,
            ModelConfig modelConfig,
            AuditService auditService,
            PlanningStepExecutor stepExecutor,
            PlanningResponseBuilder responseBuilder) {
        this.baseChatClient = baseChatClient;
        this.memoryChatClient = memoryChatClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.modelConfig = modelConfig;
        this.auditService = auditService;
        this.stepExecutor = stepExecutor;
        this.responseBuilder = responseBuilder;
    }

    public Flux<String> planStream(String task, String conversationId, boolean autoExecute, String mode) {
        return planStream(task, conversationId, autoExecute, mode, true);
    }

    public Flux<String> planStream(String task, String conversationId, boolean autoExecute, String mode, boolean memoryEnabled) {
        String resolvedConversationId = resolveConversationId(conversationId);
        return Flux.defer(() -> {
            long startTime = System.currentTimeMillis();
            try {
                String planJson = memoryEnabled
                        ? memoryChatClient.prompt()
                        .system(planningSystemPrompt)
                        .user(task)
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, resolvedConversationId))
                        .call()
                        .content()
                        : baseChatClient.prompt().system(planningSystemPrompt).user(task).call().content();
                planJson = PlanningResponseBuilder.cleanJsonResponse(planJson);
                JsonNode planNode = objectMapper.readTree(planJson);
                boolean needPlanning = planNode.path("needPlanning").asBoolean(false);
                if (!needPlanning) {
                    return responseBuilder.streamDirectAnswer(task, resolvedConversationId, memoryEnabled, startTime);
                }

                JsonNode stepsNode = planNode.path("steps");
                String taskSummary = planNode.path("taskSummary").asText("任务规划");

                List<String> stepLines = new ArrayList<>();
                for (JsonNode step : stepsNode) {
                    int num = step.path("stepNumber").asInt();
                    String desc = step.path("description").asText();
                    String tool = step.path("toolNeeded").asText("");
                    String toolLabel = tool == null || tool.isBlank() || "null".equalsIgnoreCase(tool) ? "无工具" : tool;
                    stepLines.add("步骤" + num + ": " + desc + " [" + toolLabel + "]");
                }
                if (stepLines.isEmpty()) {
                    return responseBuilder.streamDirectAnswer(task, resolvedConversationId, memoryEnabled, startTime);
                }

                boolean planOnly = !autoExecute || "planning_only".equalsIgnoreCase(mode);
                if (planOnly) {
                    String planText = "任务总结: " + taskSummary + "\n" + String.join("\n", stepLines);
                    return Flux.concat(
                            Flux.just(StreamEvent.start("开始规划任务").toJson()),
                            Flux.just(StreamEvent.delta(planText).toJson()),
                            Flux.just(StreamEvent.done("规划完成").toJson())
                    ).doFinally(signalType -> savePlanningStreamHistory(
                            resolvedConversationId,
                            task,
                            planText,
                            "planning-stream-only",
                            startTime,
                            signalType == reactor.core.publisher.SignalType.ON_COMPLETE ? "SUCCESS" : "FAILED"
                    ));
                }

                JsonNode finalStepsNode = stepsNode;
                StringBuilder finalAnswer = new StringBuilder();
                String planText = "任务总结: " + taskSummary + "\n" + String.join("\n", stepLines) + "\n\n";
                return Flux.concat(
                        Flux.just(StreamEvent.start("开始规划任务").toJson()),
                        Flux.just(StreamEvent.delta(planText).toJson()),
                        Flux.just(StreamEvent.delta("开始执行规划步骤...\n").toJson()),
                        Flux.defer(() -> {
                            List<StepResult> executedSteps = stepExecutor.executeSteps(finalStepsNode, resolvedConversationId);
                            String finalPrompt = responseBuilder.buildFinalAnswerPrompt(task, executedSteps);
                            Flux<String> answerFlux = baseChatClient.prompt()
                                    .user(finalPrompt)
                                    .stream()
                                    .content()
                                    .map(chunk -> {
                                        finalAnswer.append(chunk);
                                        return StreamEvent.delta(chunk).toJson();
                                    });
                            return Flux.concat(
                                    Flux.just(StreamEvent.delta("执行完成，正在生成最终回答...\n").toJson()),
                                    answerFlux,
                                    Flux.just(StreamEvent.done("完成").toJson())
                            );
                        })
                ).doFinally(signalType -> savePlanningStreamHistory(
                        resolvedConversationId,
                        task,
                        planText + finalAnswer,
                        "planning-stream-execute",
                        startTime,
                        signalType == reactor.core.publisher.SignalType.ON_COMPLETE ? "SUCCESS" : "FAILED"
                ));
            } catch (Exception e) {
                log.error("规划流式失败", e);
                return Flux.just(StreamEvent.error("规划失败: " + e.getMessage()).toJson());
            }
        });
    }

    public PlanningResponse planAndExecute(String task, String conversationId, boolean autoExecute) {
        return planAndExecute(task, conversationId, autoExecute, true);
    }

    @Auditable(agentType = "planning")
    public PlanningResponse planAndExecute(String task, String conversationId, boolean autoExecute, boolean memoryEnabled) {
        final String resolvedConversationId = resolveConversationId(conversationId);
        long startTime = System.currentTimeMillis();

        log.info("PlanningAgent [{}] 收到任务: {}", resolvedConversationId, task);

        String planJson = memoryEnabled
                ? memoryChatClient.prompt()
                .system(planningSystemPrompt)
                .user(task)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, resolvedConversationId))
                .call()
                .content()
                : baseChatClient.prompt().system(planningSystemPrompt).user(task).call().content();
        log.debug("规划结果 JSON: {}", planJson);

        try {
            planJson = PlanningResponseBuilder.cleanJsonResponse(planJson);
            JsonNode planNode = objectMapper.readTree(planJson);
            boolean needPlanning = planNode.path("needPlanning").asBoolean(false);

            if (!needPlanning) {
                String directAnswer = planNode.path("directAnswer").asText("无法解析回答");
                long totalTime = System.currentTimeMillis() - startTime;
                return PlanningResponse.builder().conversationId(resolvedConversationId).planned(false).directAnswer(directAnswer).totalTimeMs(totalTime).traceId(TraceContext.getTraceId()).build();
            }

            String taskSummary = planNode.path("taskSummary").asText("任务规划");
            JsonNode stepsNode = planNode.path("steps");

            if (!autoExecute) {
                List<StepResult> planSteps = new ArrayList<>();
                for (JsonNode step : stepsNode) {
                    planSteps.add(StepResult.builder().stepNumber(step.path("stepNumber").asInt()).description(step.path("description").asText()).toolUsed(step.path("toolNeeded").asText("无")).result("未执行").success(false).build());
                }
                long totalTime = System.currentTimeMillis() - startTime;
                return PlanningResponse.builder().conversationId(resolvedConversationId).taskSummary(taskSummary).planned(true).steps(planSteps).finalAnswer("仅返回规划结果，共 " + planSteps.size() + " 个步骤。").totalTimeMs(totalTime).traceId(TraceContext.getTraceId()).build();
            }

            List<StepResult> executedSteps = stepExecutor.executeSteps(stepsNode, resolvedConversationId);
            String finalAnswer = responseBuilder.generateFinalAnswer(task, executedSteps);
            long totalTime = System.currentTimeMillis() - startTime;

            return PlanningResponse.builder().conversationId(resolvedConversationId).taskSummary(taskSummary).planned(true).steps(executedSteps).finalAnswer(finalAnswer).totalTimeMs(totalTime).traceId(TraceContext.getTraceId()).build();

        } catch (Exception e) {
            log.error("任务规划解析失败，回退到直接执行模式", e);
            return responseBuilder.fallbackDirectExecution(task, resolvedConversationId, startTime, memoryEnabled);
        }
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return "plan-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return conversationId;
    }

    private void savePlanningStreamHistory(String conversationId,
                                           String task,
                                           String reply,
                                           String agentType,
                                           long startTime,
                                           String status) {
        long totalTime = System.currentTimeMillis() - startTime;
        String username = getCurrentUsername();
        auditService.saveChatHistory(conversationId, username, "user", task, agentType, modelConfig.getPrimaryModel(), null, null, 0L);
        auditService.saveChatHistory(conversationId, username, "assistant", reply == null ? "" : reply, agentType, modelConfig.getPrimaryModel(), null, null, totalTime);
        auditService.saveAgentInvocation(conversationId, agentType, modelConfig.getPrimaryModel(), task, reply, null, status, totalTime);
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())
                ? auth.getName() : "anonymous";
    }

}
