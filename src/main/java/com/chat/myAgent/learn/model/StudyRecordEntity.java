package com.chat.myAgent.learn.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "study_record", indexes = {
        @Index(name = "idx_study_user_id", columnList = "userId"),
        @Index(name = "idx_study_activity_type", columnList = "activityType"),
        @Index(name = "idx_study_created_at", columnList = "createdAt"),
        @Index(name = "idx_study_topic", columnList = "topic")
})
public class StudyRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false, unique = true)
    private String recordId;

    @Column(length = 64, nullable = false)
    private String userId;

    @Column(length = 40, nullable = false)
    private String activityType;

    @Column(length = 64)
    private String sessionId;

    @Column(length = 64)
    private String traceId;

    @Column(length = 64)
    private String noteId;

    @Column(length = 64)
    private String quizId;

    @Column(length = 64)
    private String cardId;

    @Column(length = 200)
    private String topic;

    @Column(length = 500)
    private String tags;

    @Column(length = 100)
    private String category;

    private Integer durationSeconds;
    private Integer score;
    private Integer masteryDelta;

    @Column(columnDefinition = "TEXT")
    private String detail;

    private LocalDateTime createdAt;
}
