package com.chat.myAgent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Uses a local SimpleVectorStore by default so dev startup does not require Qdrant.
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "simple")
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel,
                                         @Value("${smart-agent.rag.vector-store-path:./data/vectorstore/vector-store.json}")
                                         String vectorStorePath) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        File storeFile = Paths.get(vectorStorePath).toFile();

        ensureParentDirectory(storeFile.toPath());
        if (storeFile.exists() && storeFile.isFile()) {
            try {
                vectorStore.load(storeFile);
                log.info("Loaded local SimpleVectorStore: {}", storeFile.getAbsolutePath());
            } catch (Exception e) {
                log.warn("Failed to load local SimpleVectorStore, using an empty store: {}", e.getMessage());
            }
        } else {
            log.info("Using empty local SimpleVectorStore: {}", storeFile.getAbsolutePath());
        }

        return vectorStore;
    }

    private void ensureParentDirectory(Path storePath) {
        Path parent = storePath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (Exception e) {
            log.warn("Failed to create local vector store directory: {}, reason={}", parent, e.getMessage());
        }
    }
}
