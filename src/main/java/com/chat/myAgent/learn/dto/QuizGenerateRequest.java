package com.chat.myAgent.learn.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuizGenerateRequest {
    private String noteId;
    private String topic;
    private Integer count = 3;
    private String difficulty = "medium";
    private List<String> types;
}
