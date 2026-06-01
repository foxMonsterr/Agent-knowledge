package com.chat.myAgent.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

/**
 * 图书信息实体（结构化输出目标类）
 *
 * Spring AI 会读取 @JsonPropertyDescription 注解生成 Schema 描述
 * 帮助大模型理解每个字段应该填什么内容
 */
@Data
public class BookInfo {

    @JsonProperty("title")
    @JsonPropertyDescription("书籍名称")
    private String title;

    @JsonProperty("author")
    @JsonPropertyDescription("作者姓名")
    private String author;

    @JsonProperty("publishYear")
    @JsonPropertyDescription("出版年份，格式如 2024")
    private Integer publishYear;

    @JsonProperty("category")
    @JsonPropertyDescription("书籍分类，如：编程、文学、科学、历史等")
    private String category;

    @JsonProperty("summary")
    @JsonPropertyDescription("书籍简介，100字以内")
    private String summary;

    @JsonProperty("rating")
    @JsonPropertyDescription("推荐评分，1-10的整数")
    private Integer rating;

    @JsonProperty("targetAudience")
    @JsonPropertyDescription("目标读者群体，如：初学者、进阶开发者、所有人")
    private String targetAudience;
}
