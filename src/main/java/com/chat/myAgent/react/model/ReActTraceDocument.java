package com.chat.myAgent.react.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "react_traces")
public class ReActTraceDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String traceId;

    @Indexed
    private String userId;

    @Indexed
    private String sessionId;

    @Indexed
    private String domain;

    private String agentType;
    private String question;
    private String strategy;
    private String runtimeState;

    @Builder.Default
    private List<ReActStepDocument> steps = new ArrayList<>();

    private String finalAnswer;

    @Builder.Default
    private List<ReActSourceRefDocument> finalSources = new ArrayList<>();

    private String model;
    private Boolean fallbackUsed = false;
    private Integer totalIterations = 0;
    private Long totalDurationMs = 0L;

    @Indexed
    private String status;

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReActStepDocument {
        private String stepId;
        private Integer stepNumber;
        private String type;
        private String state;
        private String thought;
        private String actionName;
        private Map<String, Object> actionInput;
        private String observation;
        @Builder.Default
        private List<ReActSourceRefDocument> sources = new ArrayList<>();
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private Long durationMs;
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReActSourceRefDocument {
        private String sourceId;
        private String sourceType;
        private String title;
        private String snippet;
        private Double score;
        private String noteId;
        private String docId;
        private String chunkId;
    }
}
