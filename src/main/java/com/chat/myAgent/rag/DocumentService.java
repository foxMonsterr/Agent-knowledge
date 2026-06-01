package com.chat.myAgent.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * 文档服务
 *
 * 负责文档的上传、解析、切片、向量化、存储
 *
 * 处理流程：
 * 1. 接收上传文件（或读取本地文件）
 * 2. 使用 TextReader 读取文本内容
 * 3. 使用 TokenTextSplitter 将长文本切成小片段
 * 4. 为每个片段添加元数据（文件名、时间等）
 * 5. 调用 VectorStore.add() 进行向量化并存储
 * 6. 持久化向量库到本地文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VectorStore vectorStore;

    @Value("${smart-agent.rag.vector-store-path:./data/vectorstore/vector-store.json}")
    private String vectorStorePath;

    @Value("${smart-agent.rag.chunk-size:800}")
    private int chunkSize;

    @Value("${smart-agent.rag.chunk-overlap:100}")
    private int chunkOverlap;

    // 存储已入库的文档信息（简单用内存Map，后续可改为数据库）
    private final Map<String, DocumentMeta> documentRegistry = new LinkedHashMap<>();

    /**
     * 上传并入库文档（通过MultipartFile）
     */
    public DocumentMeta uploadAndIndex(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 验证文件类型
        validateFileType(fileName);

        log.info("开始处理文档: {} ({}KB)", fileName, file.getSize() / 1024);

        // 1. 读取文件内容
        Resource resource = new InputStreamResource(file.getInputStream());
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("source", fileName);
        List<Document> rawDocuments = textReader.read();

        // 2. 切片
        List<Document> chunks = splitDocuments(rawDocuments, fileName);

        // 3. 向量化并存入向量库
        vectorStore.add(chunks);

        // 4. 持久化
        persistVectorStore();

        // 5. 记录文档元信息
        DocumentMeta meta = new DocumentMeta();
        meta.setFileName(fileName);
        meta.setFileSize(file.getSize());
        meta.setChunkCount(chunks.size());
        meta.setIndexTime(new Date());
        meta.setDocumentIds(chunks.stream().map(Document::getId).toList());

        documentRegistry.put(fileName, meta);

        log.info("文档入库完成: {} → {}个片段", fileName, chunks.size());

        return meta;
    }

    /**
     * 从本地路径加载文档并入库
     */
    public DocumentMeta indexLocalFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }

        String fileName = file.getName();
        validateFileType(fileName);

        log.info("开始处理本地文档: {}", filePath);

        // 1. 读取
        Resource resource = new InputStreamResource(new FileInputStream(file));
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("source", fileName);
        textReader.getCustomMetadata().put("path", filePath);
        List<Document> rawDocuments = textReader.read();

        // 2. 切片
        List<Document> chunks = splitDocuments(rawDocuments, fileName);

        // 3. 存储
        vectorStore.add(chunks);

        // 4. 持久化
        persistVectorStore();

        // 5. 记录
        DocumentMeta meta = new DocumentMeta();
        meta.setFileName(fileName);
        meta.setFileSize(file.length());
        meta.setChunkCount(chunks.size());
        meta.setIndexTime(new Date());
        meta.setDocumentIds(chunks.stream().map(Document::getId).toList());

        documentRegistry.put(fileName, meta);

        log.info("本地文档入库完成: {} → {}个片段", fileName, chunks.size());

        return meta;
    }

    /**
     * 批量加载知识库目录下的所有文档
     */
    public List<DocumentMeta> indexKnowledgeDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("知识库目录不存在: {}", dirPath);
            return Collections.emptyList();
        }

        List<DocumentMeta> results = new ArrayList<>();
        File[] files = dir.listFiles((d, name) ->
                name.endsWith(".txt") || name.endsWith(".md"));

        if (files == null) return results;

        for (File file : files) {
            try {
                // 跳过已入库的文件
                if (documentRegistry.containsKey(file.getName())) {
                    log.debug("跳过已入库文件: {}", file.getName());
                    continue;
                }
                DocumentMeta meta = indexLocalFile(file.getAbsolutePath());
                results.add(meta);
            } catch (Exception e) {
                log.error("文件入库失败: {} - {}", file.getName(), e.getMessage());
            }
        }

        return results;
    }

    /**
     * 删除指定文档（从向量库移除）
     */
    public boolean deleteDocument(String fileName) {
        DocumentMeta meta = documentRegistry.get(fileName);
        if (meta == null) {
            return false;
        }

        // 从向量库中删除该文档的所有片段
        if (meta.getDocumentIds() != null && !meta.getDocumentIds().isEmpty()) {
            vectorStore.delete(meta.getDocumentIds());
        }

        documentRegistry.remove(fileName);
        persistVectorStore();

        log.info("文档已删除: {}", fileName);
        return true;
    }

    /**
     * 获取所有已入库文档列表
     */
    public List<DocumentMeta> listDocuments() {
        return new ArrayList<>(documentRegistry.values());
    }

    /**
     * 获取文档详情
     */
    public DocumentMeta getDocumentMeta(String fileName) {
        return documentRegistry.get(fileName);
    }

    /**
     * 文本切片
     */
    private List<Document> splitDocuments(List<Document> documents, String sourceName) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.apply(documents);

        // 为每个片段补充元数据
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            chunk.getMetadata().put("source", sourceName);
            chunk.getMetadata().put("chunk_index", i);
            chunk.getMetadata().put("total_chunks", chunks.size());
        }

        return chunks;
    }

    /**
     * 持久化向量库到本地文件
     */
    private void persistVectorStore() {
        try {
            if (vectorStore instanceof SimpleVectorStore simpleStore) {
                File storeFile = Paths.get(vectorStorePath).toFile();
                simpleStore.save(storeFile);
                log.debug("向量库已持久化: {}", vectorStorePath);
            }
        } catch (Exception e) {
            log.error("向量库持久化失败: {}", e.getMessage());
        }
    }

    /**
     * 验证文件类型
     */
    private void validateFileType(String fileName) {
        String lower = fileName.toLowerCase();
        if (!lower.endsWith(".txt") && !lower.endsWith(".md")) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 .txt 和 .md 格式。文件名: " + fileName);
        }
    }

    // ==================== 内部类：文档元信息 ====================

    @lombok.Data
    public static class DocumentMeta {
        private String fileName;
        private long fileSize;
        private int chunkCount;
        private Date indexTime;
        private List<String> documentIds;
    }
}
