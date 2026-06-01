package com.chat.myAgent.service.qdrant.impl;

import com.chat.myAgent.config.QdrantConfig;
import com.chat.myAgent.model.qdrant.SearchResult;
import com.chat.myAgent.service.qdrant.QdrantSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantSearchServiceImpl implements QdrantSearchService {

    private final QdrantConfig qdrantConfig;
    private final VectorStore vectorStore;

    @Override
    public List<SearchResult> search(String query, int topK) {
        log.info("Qdrant search 请求, collection={}, topK={}, query={}", qdrantConfig.getCollectionName(), topK, query);
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(Math.max(topK, 1))
                        .build()
        );
        return toSearchResults(documents);
    }

    @Override
    public List<SearchResult> search(String query, int topK, Map<String, Object> filter) {
        log.info("Qdrant search 请求(带过滤), collection={}, topK={}, filterKeys={}",
                qdrantConfig.getCollectionName(), topK, filter == null ? Collections.emptySet() : filter.keySet());
        List<SearchResult> results = search(query, topK);
        if (filter == null || filter.isEmpty()) {
            return results;
        }
        return results.stream()
                .filter(result -> matchesFilter(result, filter))
                .toList();
    }

    private List<SearchResult> toSearchResults(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        return documents.stream()
                .filter(Objects::nonNull)
                .map(document -> SearchResult.builder()
                        .docId(asString(document.getMetadata().get("docId")))
                        .chunkId(asString(document.getMetadata().get("chunkId")))
                        .title(asString(document.getMetadata().get("title")))
                        .content(document.getText())
                        .score(asDouble(document.getMetadata().get("score")))
                        .payload(document.getMetadata())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean matchesFilter(SearchResult result, Map<String, Object> filter) {
        Map<String, Object> payload = result.getPayload();
        if (payload == null) {
            return false;
        }
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            Object actual = payload.get(entry.getKey());
            Object expected = entry.getValue();
            if (expected == null) {
                if (actual != null) {
                    return false;
                }
                continue;
            }
            if (!expected.toString().equalsIgnoreCase(String.valueOf(actual))) {
                return false;
            }
        }
        return true;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
