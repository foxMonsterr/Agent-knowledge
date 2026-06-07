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
@Document(collection = "flashcards")
public class FlashcardDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String cardId;

    @Indexed
    private String userId;

    @Indexed
    private String noteId;

    private String front;
    private String back;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private String category;
    private Double easeFactor = 2.5;
    private Integer intervalDays = 1;
    private Integer reviewCount = 0;
    private Integer lapseCount = 0;
    private LocalDateTime lastReviewAt;

    @Indexed
    private LocalDateTime nextReviewAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
