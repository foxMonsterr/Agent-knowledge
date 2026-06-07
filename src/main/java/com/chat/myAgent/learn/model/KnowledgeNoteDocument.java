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
@Document(collection = "knowledge_notes")
public class KnowledgeNoteDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String noteId;

    @Indexed
    private String userId;

    private String title;
    private String content;
    private String summary;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Indexed
    private String category;

    private String sourceType;
    private String sourceDocId;
    private String sourceTraceId;

    @Builder.Default
    private List<String> relatedNoteIds = new ArrayList<>();

    @Indexed
    private Integer masteryLevel = 0;

    private Integer reviewCount = 0;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewAt;
    private Boolean vectorIndexed = false;

    @Builder.Default
    private List<String> vectorIds = new ArrayList<>();

    @Indexed
    private Boolean archived = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
