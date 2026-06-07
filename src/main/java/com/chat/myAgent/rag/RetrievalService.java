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
 *
 * 设计要点:
 * - 检索时按 userId 过滤(多租户隔离)
 * - 设相似度阈值(过滤掉"看起来像但其实不相关"的结果)
 * - 过滤 enabled=false 的片段(软删除)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final VectorStore vectorStore;

    // 默认返回 Top-5
    @Value("${smart-agent.rag.top-k:5}")
    private int defaultTopK;

    // 默认相似度阈值 0.5(余弦相似度,范围 [0,1])
    @Value("${smart-agent.rag.similarity-threshold:0.5}")
    private double defaultSimilarityThreshold;

    /**
     * 相似度检索（使用默认参数）
     * 注意:此方法检索的是"系统级"文档(管理员批量导入的)
     * 普通用户检索应该用 retrieveForUser()
     */
    public List<Document> retrieve(String query) {
        return retrieve(query, defaultTopK, defaultSimilarityThreshold);
    }

    /**
     * 相似度检索（自定义参数）
     *
     * @param query              用户问题
     * @param topK              返回最相关的K个片段
     * @param similarityThreshold 相似度阈值（0-1，低于此值不返回）
     */
    public List<Document> retrieve(String query, int topK, double similarityThreshold) {
        log.debug("开始检索 query='{}', topK={}, threshold={}", query, topK, similarityThreshold);

        // Spring AI 的 SearchRequest 是"查询向量 + topK + 过滤条件"的统一封装
        // filterExpression 是 Qdrant 的过滤语法(类似 SQL WHERE)
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                // 管理员导入的文档 userId='system',与普通用户隔离
                .filterExpression("userId == 'system'")
                .build();

        // vectorStore.similaritySearch() 内部会:
        // 1. 把 query 转成 Embedding 向量
        // 2. 在 Qdrant 中算余弦相似度 + 过滤 userId
        // 3. 按相似度降序取 topK
        List<Document> results = filterEnabled(vectorStore.similaritySearch(searchRequest));

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
     * 按用户检索(LearnAgent 专用)
     * 与 retrieve() 区别:把 userId='system' 改成实际用户 ID
     */
    public List<Document> retrieveForUser(String userId, String query, int topK, double similarityThreshold) {
        log.debug("开始按用户检索 userId='{}', query='{}', topK={}, threshold={}", userId, query, topK, similarityThreshold);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                // escapeFilterValue 防止 userId 含单引号破坏 filter 语法
                .filterExpression("userId == '" + escapeFilterValue(userId) + "'")
                .build();
        List<Document> results = filterEnabled(vectorStore.similaritySearch(searchRequest));
        log.debug("用户检索到 {} 个相关片段", results.size());
        return results;
    }

    /**
     * 检索并格式化为可读文本（用于调试和展示）
     * 返回示例:
     *   检索到 3 个相关片段:
     *   --- 片段 1 ---
     *   来源: xx.pdf (第2段)
     *   内容: ...
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
     * 用于"我找到了来自这些文档的答案"展示
     */
    public List<String> getSourceFiles(List<Document> documents) {
        return documents.stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("source", "未知"))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 转义 filter 表达式中的单引号(防止 SQL 注入式攻击)
     * 例如 userId="o'le" → filter: "userId == 'o\\'le'"
     */
    private String escapeFilterValue(String value) {
        return value == null ? "" : value.replace("'", "\\'");
    }

    /**
     * 过滤掉 enabled=false 的片段(软删除)
     * 注意:null 视为启用(向后兼容旧数据)
     */
    private List<Document> filterEnabled(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .filter(document -> document != null && isEnabled(document.getMetadata().get("enabled")))
                .toList();
    }

    /**
     * 启用判断:兼容 Boolean 类型和 String 类型
     * 历史数据可能存为 String "true"/"false",新数据存为 Boolean
     */
    private boolean isEnabled(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return !"false".equalsIgnoreCase(String.valueOf(value));
    }
}
