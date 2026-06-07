package com.chat.myAgent.agent.planning;

import com.chat.myAgent.model.vo.StepResult;
import com.chat.myAgent.tool.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PlanningStepExecutor {

    private final ChatClient baseChatClient;
    private final DateTimeTool dateTimeTool;
    private final CalculatorTool calculatorTool;
    private final TranslateTool translateTool;
    private final DocParseTool docParseTool;
    private final DbQueryTool dbQueryTool;

    public PlanningStepExecutor(
            @Qualifier("baseChatClient") ChatClient baseChatClient,
            DateTimeTool dateTimeTool,
            CalculatorTool calculatorTool,
            TranslateTool translateTool,
            DocParseTool docParseTool,
            DbQueryTool dbQueryTool) {
        this.baseChatClient = baseChatClient;
        this.dateTimeTool = dateTimeTool;
        this.calculatorTool = calculatorTool;
        this.translateTool = translateTool;
        this.docParseTool = docParseTool;
        this.dbQueryTool = dbQueryTool;
    }

    public List<StepResult> executeSteps(JsonNode stepsNode, String conversationId) {
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
            results.add(StepResult.builder()
                    .stepNumber(stepNumber)
                    .description(description)
                    .toolUsed(toolNeeded)
                    .result(stepResult)
                    .success(success)
                    .timeMs(stepTime)
                    .build());
        }
        return results;
    }

    private String buildStepPrompt(String stepDescription, String previousContext) {
        StringBuilder sb = new StringBuilder();
        if (!previousContext.isBlank()) sb.append("之前步骤的执行结果：\n").append(previousContext).append("\n\n");
        sb.append("当前任务：").append(stepDescription);
        sb.append("\n请执行上述任务并返回结果。");
        return sb.toString();
    }

    private String executeWithTools(String prompt, String toolName) {
        return baseChatClient.prompt().user(prompt).tools(resolveTools(toolName)).call().content();
    }

    private Object[] resolveTools(String toolName) {
        if (toolName == null || toolName.isBlank() || "null".equalsIgnoreCase(toolName) || "无".equals(toolName)) {
            return new Object[]{ dateTimeTool, calculatorTool, translateTool, docParseTool, dbQueryTool };
        }
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
}
