package com.chat.myAgent.learn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuizEvaluateRequest {
    @NotBlank(message = "quizId不能为空")
    private String quizId;
    @NotBlank(message = "答案不能为空")
    private String userAnswer;
}
