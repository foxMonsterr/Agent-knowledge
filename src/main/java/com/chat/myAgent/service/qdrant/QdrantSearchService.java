package com.chat.myAgent.service.qdrant;

import com.chat.myAgent.model.qdrant.SearchResult;

import java.util.List;
import java.util.Map;

public interface QdrantSearchService {

    List<SearchResult> search(String query, int topK);

    List<SearchResult> search(String query, int topK, Map<String, Object> filter);
}
