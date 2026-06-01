package com.chat.myAgent.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

/**
 * 任务分析实体
 *
 * 用于将用户的自然语言需求拆解为结构化的任务描述
 * 这是阶段5「PlanningAgent」的数据基础
 */
@Data
public class TaskAnalysis {

    @JsonProperty("taskType")
    @JsonPropertyDescription("任务类型：question(问答)/coding(编程)/translation(翻译)/analysis(分析)/creative(创作)")
    private String taskType;

    @JsonProperty("complexity")
    @JsonPropertyDescription("复杂度：simple(简单)/medium(中等)/complex(复杂)")
    private String complexity;

    @JsonProperty("summary")
    @JsonPropertyDescription("任务摘要，一句话描述核心需求")
    private String summary;

    @JsonProperty("steps")
    @JsonPropertyDescription("建议的执行步骤列表")
    private List<String> steps;

    @JsonProperty("toolsNeeded")
    @JsonPropertyDescription("可能需要的工具列表，如：calculator、translator、doc_parser、db_query、datetime")
    private List<String> toolsNeeded;

    @JsonProperty("estimatedTime")
    @JsonPropertyDescription("预估完成时间，如：几秒、几分钟")
    private String estimatedTime;
}
