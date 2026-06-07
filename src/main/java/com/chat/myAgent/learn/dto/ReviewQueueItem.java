package com.chat.myAgent.learn.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewQueueItem {
    private String cardId;
    private String noteId;
    private String front;
    private String back;
    private String title;
    private Double easeFactor;
    private Integer intervalDays;
    private Integer reviewCount;
    private Integer masteryLevel;
    private LocalDateTime nextReviewAt;
    private Long overdueDays;
    private Double priority;
}
