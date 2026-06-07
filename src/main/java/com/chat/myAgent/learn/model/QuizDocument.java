package com.chat.myAgent.learn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quizzes")
public class QuizDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String quizId;

    @Indexed
    private String quizSetId;

    @Indexed
    private String userId;

    @Indexed
    private String noteId;

    private String sourceTraceId;
    private String type;
    private String question;

    @Builder.Default
    private List<QuizOption> options = new ArrayList<>();

    private String correctAnswer;
    private String explanation;
    private String difficulty;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private String category;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizOption {
        private String key;
        private String text;
    }
}
