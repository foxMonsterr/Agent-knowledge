package com.chat.myAgent.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

/**
 * 情感分析结果实体
 */
@Data
public class SentimentResult {

    @JsonProperty("sentiment")
    @JsonPropertyDescription("情感倾向：positive(正面)/negative(负面)/neutral(中性)")
    private String sentiment;

    @JsonProperty("confidence")
    @JsonPropertyDescription("置信度，0.0到1.0之间的小数")
    private Double confidence;

    @JsonProperty("keywords")
    @JsonPropertyDescription("关键情感词汇列表")
    private java.util.List<String> keywords;

    @JsonProperty("explanation")
    @JsonPropertyDescription("简短的分析解释，50字以内")
    private String explanation;
}
