package com.chat.myAgent.service.qdrant;

import com.chat.myAgent.model.qdrant.DocumentChunk;

import java.util.List;

public interface QdrantIndexService {

    void createCollectionIfAbsent();

    void upsertChunks(List<DocumentChunk> chunks);
}
