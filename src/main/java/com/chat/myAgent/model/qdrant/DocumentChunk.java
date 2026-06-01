package com.chat.myAgent.model.qdrant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    private String docId;
    private String chunkId;
    private String title;
    private String content;
    private String source;
    private String category;
    private List<String> tags;
    private Integer chunkIndex;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
