package com.chat.myAgent.model.qdrant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private String docId;
    private String chunkId;
    private String title;
    private String content;
    private Double score;
    private Map<String, Object> payload;
}
