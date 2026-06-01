package com.chat.myAgent.service.qdrant.impl;

import com.chat.myAgent.config.QdrantConfig;
import com.chat.myAgent.model.qdrant.DocumentChunk;
import com.chat.myAgent.service.qdrant.QdrantIndexService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantIndexServiceImpl implements QdrantIndexService {

    private final QdrantConfig qdrantConfig;
    private final VectorStore vectorStore;
    private final QdrantClient qdrantClient;

    @Override
    public void createCollectionIfAbsent() {
        String collectionName = qdrantConfig.getCollectionName();
        log.info("检查/创建 Qdrant collection: {}@{}:{}", collectionName, qdrantConfig.getHost(), qdrantConfig.getPort());
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (!exists) {
                qdrantClient.createCollectionAsync(collectionName,
                        Collections.VectorParams.newBuilder()
                                .setSize(1024)
                                .setDistance(Collections.Distance.Cosine)
                                .build()).get();
                log.info("Qdrant collection 创建成功: {}", collectionName);
            } else {
                log.info("Qdrant collection 已存在: {}", collectionName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("创建 Qdrant collection 被中断");
        } catch (ExecutionException e) {
            log.warn("创建 Qdrant collection 失败（可能已存在或自动初始化已处理）: {}", e.getMessage());
        }
    }

    @PostConstruct
    void init() {
        createCollectionIfAbsent();
    }

    @Override
    public void upsertChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("Qdrant upsertChunks 收到空数据，直接返回");
            return;
        }

        List<Document> documents = chunks.stream()
                .map(this::toDocument)
                .collect(Collectors.toList());

        log.info("准备向 Qdrant 批量写入 chunk, size={}", documents.size());
        vectorStore.add(documents);
    }

    private Document toDocument(DocumentChunk chunk) {
        Map<String, Object> metadata = Map.of(
                "docId", nullSafe(chunk.getDocId()),
                "chunkId", nullSafe(chunk.getChunkId()),
                "title", nullSafe(chunk.getTitle()),
                "source", nullSafe(chunk.getSource()),
                "category", nullSafe(chunk.getCategory()),
                "tags", chunk.getTags() == null ? List.of() : chunk.getTags(),
                "chunkIndex", chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex(),
                "enabled", chunk.getEnabled() == null ? Boolean.TRUE : chunk.getEnabled(),
                "createdAt", chunk.getCreatedAt() == null ? null : chunk.getCreatedAt().toString(),
                "updatedAt", chunk.getUpdatedAt() == null ? null : chunk.getUpdatedAt().toString()
        );
        return new Document(chunk.getContent(), metadata);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
