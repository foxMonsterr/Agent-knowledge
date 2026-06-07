package com.chat.myAgent.learn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "learning_paths")
public class LearningPathDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String pathId;

    @Indexed
    private String userId;

    private String topic;
    private Integer targetNoteCount;
    private String preferredDepth;

    @Builder.Default
    private String status = "active";

    @Builder.Default
    private Integer completedStages = 0;

    private Integer totalStages;

    private List<LearningStageDocument> stages;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningStageDocument {
        private String stageId;
        private Integer order;
        private String title;
        private String description;
        private String action;
        private String resourceNoteId;
        @Builder.Default
        private String status = "pending";
        @Builder.Default
        private Integer score = 0;
        private LocalDateTime completedAt;
    }
}
