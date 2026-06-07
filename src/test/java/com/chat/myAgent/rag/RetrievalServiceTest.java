package com.chat.myAgent.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private VectorStore vectorStore;

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new RetrievalService(vectorStore);
        ReflectionTestUtils.setField(retrievalService, "defaultTopK", 5);
        ReflectionTestUtils.setField(retrievalService, "defaultSimilarityThreshold", 0.5);
    }

    @Test
    void retrieveForUserBuildsUserFilterAndExcludesDisabledChunks() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                doc("enabled result", true),
                doc("disabled result", false),
                doc("string disabled result", "false")
        ));

        List<Document> results = retrievalService.retrieveForUser("user-1", "机器学习", 3, 0.7);

        assertThat(results)
                .extracting(Document::getText)
                .containsExactly("enabled result");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertThat(request.getQuery()).isEqualTo("机器学习");
        assertThat(request.getTopK()).isEqualTo(3);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.7);
        assertThat(request.getFilterExpression()).isNotNull();
        assertThat(request.getFilterExpression().toString()).contains("userId", "user-1");
    }

    @Test
    void retrieveUsesSystemScopeByDefault() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                doc("system result", null)
        ));

        List<Document> results = retrievalService.retrieve("RAG");

        assertThat(results).hasSize(1);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.5);
        assertThat(request.getFilterExpression()).isNotNull();
        assertThat(request.getFilterExpression().toString()).contains("userId", "system");
    }

    private Document doc(String text, Object enabled) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "test.md");
        if (enabled != null) {
            metadata.put("enabled", enabled);
        }
        return new Document(text, metadata);
    }
}

