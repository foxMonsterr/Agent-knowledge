package com.chat.myAgent.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量检索服务
 *
 * 负责从向量库中检索与用户问题最相关的文档片段
 *
 * 工作原理：
 * 1. 将用户问题通过 EmbeddingModel 转为向量
 * 2. 在向量库中计算余弦相似度
 * 3. 返回相似度最高的 Top-K 个片段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final VectorStore vectorStore;

    @Value("${smart-agent.rag.top-k:5}")
    private int defaultTopK;

    @Value("${smart-agent.rag.similarity-threshold:0.5}")
    private double defaultSimilarityThreshold;

    /**
     * 相似度检索（使用默认参数）
     */
    public List<Document> retrieve(String query) {
        return retrieve(query, defaultTopK, defaultSimilarityThreshold);
    }

    /**
     * 相似度检索（自定义参数）
     *
     * @param query              用户问题
     * @param topK              0 返回最相关的K个片段
     * @param similarityThreshold 相似度阈值（0-1，低于此值不返回）
     */
    public List<Document> retrieve(String query, int topK, double similarityThreshold) {
        log.debug("开始检索 query='{}', topK={}, threshold={}", query, topK, similarityThreshold);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        log.debug("检索到 {} 个相关片段", results.size());

        if (log.isTraceEnabled()) {
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                log.trace("片段[{}] source={}, content={}...",
                        i, doc.getMetadata().get("source"),
                        doc.getText().substring(0, Math.min(50, doc.getText().length())));
            }
        }

        return results;
    }

    /**
     * 检索并格式化为可读文本（用于调试和展示）
     */
    public String retrieveFormatted(String query) {
        List<Document> docs = retrieve(query);

        if (docs.isEmpty()) {
            return "未找到相关文档片段";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("检索到 ").append(docs.size()).append(" 个相关片段：\n\n");

        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String source = (String) doc.getMetadata().getOrDefault("source", "未知");
            Object chunkIndex = doc.getMetadata().getOrDefault("chunk_index", "?");

            sb.append("--- 片段 ").append(i + 1).append(" ---\n");
            sb.append("来源: ").append(source).append(" (第").append(chunkIndex).append("段)\n");
            sb.append("内容: ").append(doc.getText()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 获取检索结果的来源文件列表（去重）
     */
    public List<String> getSourceFiles(List<Document> documents) {
        return documents.stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("source", "未知"))
                .distinct()
                .collect(Collectors.toList());
    }
}
