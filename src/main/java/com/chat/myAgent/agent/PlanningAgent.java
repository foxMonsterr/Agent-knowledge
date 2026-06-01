package com.chat.myAgent.agent;

import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.common.stream.StreamEvent;
import com.chat.myAgent.config.ModelConfig;
import com.chat.myAgent.model.vo.PlanningResponse;
import com.chat.myAgent.model.vo.StepResult;
import com.chat.myAgent.service.AuditService;
import com.chat.myAgent.tool.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class PlanningAgent {

    private final ChatClient baseChatClient;
    private final ChatClient fullAgentClient;
    private final ObjectMapper objectMapper;
    private final ModelConfig modelConfig;
    private final DateTimeTool dateTimeTool;
    private final CalculatorTool calculatorTool;
    private final TranslateTool translateTool;
    private final DocParseTool docParseTool;
    private final DbQueryTool dbQueryTool;
    private final AuditService auditService;

    @Value("classpath:prompts/planning-system.st")
    private Resource planningSystemPrompt;

    public PlanningAgent(
            @Qualifier("baseChatClient") ChatClient baseChatClient,
            @Qualifier("fullAgentClient") ChatClient fullAgentClient,
            ModelConfig modelConfig,
            DateTimeTool dateTimeTool,
            CalculatorTool calculatorTool,
            TranslateTool translateTool,
            DocParseTool docParseTool,
            DbQueryTool dbQueryTool,
            AuditService auditService) {
        this.baseChatClient = baseChatClient;
        this.fullAgentClient = fullAgentClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.modelConfig = modelConfig;
        this.dateTimeTool = dateTimeTool;
        this.calculatorTool = calculatorTool;
        this.translateTool = translateTool;
        this.docParseTool = docParseTool;
        this.dbQueryTool = dbQueryTool;
        this.auditService = auditService;
    }

    public Flux<String> planStream(String task, String conversationId, boolean autoExecute, String mode) {
        String resolvedConversationId = resolveConversationId(conversationId);
        return Flux.defer(() -> {
            try {
                String planJson = baseChatClient.prompt().system(planningSystemPrompt).user(task).call().content();
                planJson = cleanJsonResponse(planJson);
                JsonNode planNode = objectMapper.readTree(planJson);
                JsonNode stepsNode = planNode.path("steps");

                List<String> stepLines = new ArrayList<>();
                for (JsonNode step : stepsNode) {
                    int num = step.path("stepNumber").asInt();
                    String desc = step.path("description").asText();
                    stepLines.add("步骤" + num + ": " + desc);
                }
                if (stepLines.isEmpty()) {
                    stepLines.add("直接执行任务");
                }

                return Flux.concat(
                        Flux.just(StreamEvent.start("开始规划任务").toJson()),
                        Flux.fromIterable(stepLines).map(s -> StreamEvent.delta(s).toJson()),
                        Flux.just(StreamEvent.done("规划完成").toJson())
                ).doOnComplete(() -> auditService.saveAgentInvocation(
                        resolvedConversationId,
                        "planning-stream",
                        modelConfig.getPrimaryModel(),
                        task,
                        "streamed",
                        null,
                        "SUCCESS",
                        0L));
            } catch (Exception e) {
                log.error("规划流式失败", e);
                return Flux.just(StreamEvent.error("规划失败: " + e.getMessage()).toJson());
            }
        });
    }

    public PlanningResponse planAndExecute(String task, String conversationId, boolean autoExecute) {
        final String resolvedConversationId = resolveConversationId(conversationId);
        long startTime = System.currentTimeMillis();

        log.info("PlanningAgent [{}] 收到任务: {}", resolvedConversationId, task);

        String planJson = baseChatClient.prompt().system(planningSystemPrompt).user(task).call().content();
        log.debug("规划结果 JSON: {}", planJson);

        try {
            planJson = cleanJsonResponse(planJson);
            JsonNode planNode = objectMapper.readTree(planJson);
            boolean needPlanning = planNode.path("needPlanning").asBoolean(false);

            if (!needPlanning) {
                String directAnswer = planNode.path("directAnswer").asText("无法解析回答");
                long totalTime = System.currentTimeMillis() - startTime;
                PlanningResponse response = PlanningResponse.builder().conversationId(resolvedConversationId).planned(false).directAnswer(directAnswer).totalTimeMs(totalTime).traceId(TraceContext.getTraceId()).build();
                auditService.saveAgentInvocation(resolvedConversationId, "planning-direct", modelConfig.getPrimaryModel(), task, response.getDirectAnswer(), null, "SUCCESS", totalTime);
                return response;
            }

            String taskSummary = planNode.path("taskSummary").asText("任务规划");
            JsonNode stepsNode = planNode.path("steps");

            if (!autoExecute) {
                List<StepResult> planSteps = new ArrayList<>();
                for (JsonNode step : stepsNode) {
                    planSteps.add(StepResult.builder().stepNumber(step.path("stepNumber").asInt()).description(step.path("description").asText()).toolUsed(step.path("toolNeeded").asText("无")).result("未执行").success(false).build());
                }
                long totalTime = System.currentTimeMillis() - startTime;
                PlanningResponse response = PlanningResponse.builder().conversationId(resolvedConversationId).taskSummary(taskSummary).planned(true).steps(planSteps).finalAnswer("仅返回规划结果，共 " + planSteps.size() + " 个步骤。").totalTimeMs(totalTime).traceId(TraceContext.getTraceId()).build();
                auditService.saveAgentInvocation(resolvedConversationId, "planning-only", modelConfig.getPrimaryModel(), task, response.getFinalAnswer(), null, "SUCCESS", totalTime);
                return response;
            }

            List<StepResult> executedSteps = executeSteps(stepsNode, resolvedConversationId);
            String finalAnswer = generateFinalAnswer(task, executedSteps);
            long totalTime = System.currentTimeMillis() - startTime;

            PlanningResponse response = PlanningResponse.builder().conversationId(resolvedConversationId).taskSummary(taskSummary).planned(true).steps(executedSteps).finalAnswer(finalAnswer).totalTimeMs(totalTime).traceId(TraceContext.getTraceId()).build();
            auditService.saveAgentInvocation(resolvedConversationId, "planning-execute", modelConfig.getPrimaryModel(), task, response.getFinalAnswer(), null, "SUCCESS", totalTime);
            return response;

        } catch (Exception e) {
            log.error("任务规划解析失败，回退到直接执行模式", e);
            return fallbackDirectExecution(task, resolvedConversationId, startTime);
        }
    }

    private List<StepResult> executeSteps(JsonNode stepsNode, String conversationId) { /* unchanged */
        List<StepResult> results = new ArrayList<>();
        StringBuilder contextAccumulator = new StringBuilder();
        for (JsonNode step : stepsNode) {
            int stepNumber = step.path("stepNumber").asInt();
            String description = step.path("description").asText();
            String toolNeeded = step.path("toolNeeded").asText("无");
            long stepStart = System.currentTimeMillis();
            String stepResult;
            boolean success;
            try {
                String stepPrompt = buildStepPrompt(description, contextAccumulator.toString());
                if (toolNeeded != null && !toolNeeded.isBlank() && !toolNeeded.equals("null") && !toolNeeded.equals("无")) {
                    stepResult = executeWithTools(stepPrompt, toolNeeded);
                } else {
                    stepResult = baseChatClient.prompt().user(stepPrompt).call().content();
                }
                success = true;
            } catch (Exception e) {
                stepResult = "步骤执行失败: " + e.getMessage();
                success = false;
            }
            long stepTime = System.currentTimeMillis() - stepStart;
            contextAccumulator.append("\n[步骤").append(stepNumber).append("结果]: ").append(stepResult);
            results.add(StepResult.builder().stepNumber(stepNumber).description(description).toolUsed(toolNeeded).result(stepResult).success(success).timeMs(stepTime).build());
        }
        return results;
    }

    private String executeWithTools(String prompt, String toolName) {
        return baseChatClient.prompt().user(prompt).tools(resolveTools(toolName)).call().content();
    }

    private Object[] resolveTools(String toolName) {
        List<Object> tools = new ArrayList<>();
        String name = toolName.toLowerCase().trim();
        if (name.contains("datetime") || name.contains("time") || name.contains("date")) tools.add(dateTimeTool);
        if (name.contains("calc") || name.contains("math") || name.contains("计算")) tools.add(calculatorTool);
        if (name.contains("translate") || name.contains("翻译")) tools.add(translateTool);
        if (name.contains("doc") || name.contains("file") || name.contains("文件")) tools.add(docParseTool);
        if (name.contains("db") || name.contains("database") || name.contains("查询")) tools.add(dbQueryTool);
        if (tools.isEmpty()) {
            tools.add(dateTimeTool);
            tools.add(calculatorTool);
            tools.add(translateTool);
            tools.add(docParseTool);
            tools.add(dbQueryTool);
        }
        return tools.toArray();
    }

    private String buildStepPrompt(String stepDescription, String previousContext) {
        StringBuilder sb = new StringBuilder();
        if (!previousContext.isBlank()) sb.append("之前步骤的执行结果：\n").append(previousContext).append("\n\n");
        sb.append("当前任务：").append(stepDescription);
        sb.append("\n请执行上述任务并返回结果。");
        return sb.toString();
    }

    private String generateFinalAnswer(String originalTask, List<StepResult> steps) {
        StringBuilder context = new StringBuilder();
        context.append("用户原始需求：").append(originalTask).append("\n\n");
        context.append("各步骤执行结果：\n");
        for (StepResult step : steps) {
            context.append("步骤").append(step.getStepNumber()).append(": ").append(step.getDescription()).append("\n");
            context.append("结果: ").append(step.getResult()).append("\n");
            context.append("状态: ").append(step.isSuccess() ? "✅成功" : "❌失败").append("\n\n");
        }
        context.append("请基于以上所有步骤的执行结果，生成一个完整、连贯的最终回答给用户。");
        return baseChatClient.prompt().user(context.toString()).call().content();
    }

    private PlanningResponse fallbackDirectExecution(String task, String conversationId, long startTime) {
        String reply = fullAgentClient.prompt().user(task).tools(dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool).advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId)).call().content();
        long totalTime = System.currentTimeMillis() - startTime;
        PlanningResponse response = PlanningResponse.builder().conversationId(conversationId).planned(false).directAnswer(reply).finalAnswer(reply).totalTimeMs(totalTime).traceId(TraceContext.getTraceId()).build();
        auditService.saveAgentInvocation(conversationId, "planning-fallback", modelConfig.getPrimaryModel(), task, response.getFinalAnswer(), null, "SUCCESS", totalTime);
        return response;
    }

    private String cleanJsonResponse(String json) {
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

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return "plan-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return conversationId;
    }

}
