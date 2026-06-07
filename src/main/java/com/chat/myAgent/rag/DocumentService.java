package com.chat.myAgent.rag;

import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 文档服务：负责资料解析、切片、向量化和向量删除。
 *
 * 核心职责：
 * 1. 解析 PDF / DOCX / TXT / MD 为纯文本
 * 2. 用 TokenTextSplitter 按 token 切分成短片段
 * 3. 给每个片段打"业务元数据"(userId/category/tags/source...)
 * 4. 写入向量库(Qdrant 或 SimpleVectorStore)
 * 5. 维护 documentRegistry(docId → DocumentMeta)用于后续删除/查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    // 向量库抽象(Spring AI 的统一接口,可以是 Qdrant 或 SimpleVectorStore)
    private final VectorStore vectorStore;

    // SimpleVectorStore 持久化路径(开发环境用)
    @Value("${smart-agent.rag.vector-store-path:./data/vectorstore/vector-store.json}")
    private String vectorStorePath;

    // 每个片段的目标 token 数(由 @Value 注入,默认 800)
    @Value("${smart-agent.rag.chunk-size:${learn-agent.note.chunk-size:800}}")
    private int chunkSize;

    // 切分时相邻片段的重叠 token 数(防止关键信息被切在边界)
    @Value("${smart-agent.rag.chunk-overlap:${learn-agent.note.chunk-overlap:100}}")
    private int chunkOverlap;

    // 文档注册表:支持按 docId 和按 fileName 两种 key 查询
    // 用 LinkedHashMap 保留插入顺序,持久化时按入库时间排序
    private final Map<String, DocumentMeta> documentRegistry = new LinkedHashMap<>();
    // 注册表需要序列化到磁盘,用到 JavaTimeModule 序列化 LocalDateTime
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * 应用启动时从磁盘加载注册表(避免重启后丢失)
     * 加载失败时不抛异常,而是降级到空注册表(不影响服务启动)
     */
    @PostConstruct
    public void loadRegistry() {
        try {
            File registryFile = registryFilePath();
            if (registryFile.exists()) {
                List<DocumentMeta> list = objectMapper.readValue(registryFile, new TypeReference<List<DocumentMeta>>() {});
                for (DocumentMeta meta : list) {
                    documentRegistry.put(meta.getDocId(), meta);
                    documentRegistry.put(meta.getFileName(), meta);
                }
                log.info("已从磁盘加载文档注册表，共 {} 个文档", list.size());
            }
        } catch (Exception e) {
            log.warn("加载文档注册表失败，将从空白注册表开始: {}", e.getMessage());
        }
    }

    /**
     * 把注册表写到磁盘
     * 用 LinkedHashSet 去重(因为按 docId 和 fileName 都放了一份)
     */
    private void persistRegistry() {
        try {
            List<DocumentMeta> unique = new ArrayList<>(
                    new LinkedHashSet<>(documentRegistry.values()));
            objectMapper.writeValue(registryFilePath(), unique);
        } catch (Exception e) {
            log.error("持久化文档注册表失败: {}", e.getMessage());
        }
    }

    /**
     * 注册表文件路径 = 向量库目录/document-registry.json
     * 复用 vectorStorePath 的目录,避免再开一个新目录
     */
    private File registryFilePath() {
        String storeDir = vectorStorePath.replace('\\', '/');
        int lastSlash = storeDir.lastIndexOf('/');
        String dir = lastSlash < 0 ? "." : storeDir.substring(0, lastSlash);
        return Paths.get(dir, "document-registry.json").toFile();
    }

    /**
     * 简化版上传(无 userId/分类/标签,用于管理员批量导入)
     */
    public DocumentMeta uploadAndIndex(MultipartFile file) throws IOException {
        return uploadAndIndex(file, null, "document", List.of(), null);
    }

    /**
     * 完整版上传:解析 → 切分 → 元数据增强 → 向量化 → 入库
     * 入库流程是 RAG 离线索引阶段的核心,每一步都是为了后续检索精度服务
     */
    public DocumentMeta uploadAndIndex(MultipartFile file, String userId, String sourceType,
                                       List<String> tags, String category) throws IOException {
        // 1. 文件预处理:清理文件名 + 校验类型
        String fileName = sanitizeFileName(file.getOriginalFilename());
        validateFileType(fileName);
        log.info("开始处理文档: {} ({}KB)", fileName, file.getSize() / 1024);

        byte[] bytes = file.getBytes();
        // 2. 生成 docId(32位UUID,URL安全)
        String docId = "doc-" + UUID.randomUUID().toString().replace("-", "");
        // 3. 解析文档为 Document 列表(每个 Document 代表一页/一节)
        List<Document> rawDocuments = readDocuments(fileName, bytes);
        String textContent = joinText(rawDocuments);

        // 4. 构建基础元数据(每个片段都会继承这些字段)
        Map<String, Object> metadata = baseMetadata(userId, sourceType, fileName, docId, null, category, tags);
        // 5. 切分 + 元数据增强(详见 splitAndEnrich 注释)
        List<Document> chunks = splitAndEnrich(rawDocuments, fileName, metadata);
        // 6. 写入向量库(Embedding 模型会自动把 text 转成向量)
        vectorStore.add(chunks);
        // 7. SimpleVectorStore 需要显式持久化(Qdrant 不用,这里走 if 分支)
        persistVectorStore();

        // 8. 注册到 documentRegistry(方便后续按 docId 删除)
        DocumentMeta meta = buildMeta(docId, fileName, file.getSize(), chunks, textContent);
        documentRegistry.put(docId, meta);
        documentRegistry.put(fileName, meta);
        persistRegistry();
        log.info("文档入库完成: {} -> {}个片段", fileName, chunks.size());
        return meta;
    }

    /**
     * 把单条笔记向量化入库(笔记本身就是短文本,通常只切成 1-3 个片段)
     * 与 uploadAndIndex 区别:输入是 KnowledgeNoteDocument 不是 MultipartFile
     */
    public List<String> indexNote(KnowledgeNoteDocument note) {
        if (note == null || Boolean.TRUE.equals(note.getArchived())) {
            return List.of();
        }
        String noteId = note.getNoteId();
        String text = noteText(note);
        if (text.isBlank()) {
            return List.of();
        }

        // noteId 和 docId 都要有:docId 用于"按笔记删向量",noteId 用于反查
        Map<String, Object> metadata = baseMetadata(note.getUserId(), "note", note.getTitle(),
                "note-doc-" + noteId, noteId, note.getCategory(), note.getTags());
        metadata.put("title", note.getTitle());
        metadata.put("createdAt", asString(note.getCreatedAt()));
        metadata.put("updatedAt", asString(note.getUpdatedAt()));

        Document raw = new Document("note-raw-" + noteId, text, metadata);
        List<Document> chunks = splitAndEnrich(List.of(raw), safeSourceName(note.getTitle()), metadata);
        vectorStore.add(chunks);
        persistVectorStore();
        // 返回所有片段的 ID,调用方存到 note.vectorIds 用于后续删除
        return chunks.stream().map(Document::getId).toList();
    }

    /**
     * 按向量 ID 批量删除
     * 注意:不会从 documentRegistry 移除(因为这里只删了部分片段)
     * 删整个文档请用 deleteDocument(docId)
     */
    public void deleteVectors(List<String> vectorIds) {
        if (vectorIds == null || vectorIds.isEmpty()) {
            return;
        }
        try {
            vectorStore.delete(vectorIds.stream().filter(id -> id != null && !id.isBlank()).toList());
            persistVectorStore();
        } catch (Exception e) {
            log.warn("删除向量失败: {}", e.getMessage());
        }
    }

    /**
     * 索引本地文件(管理员批量导入时使用)
     */
    public DocumentMeta indexLocalFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        String fileName = sanitizeFileName(file.getName());
        validateFileType(fileName);
        byte[] bytes = Files.readAllBytes(file.toPath());
        String docId = "doc-" + UUID.randomUUID().toString().replace("-", "");
        List<Document> rawDocuments = readDocuments(fileName, bytes);
        Map<String, Object> metadata = baseMetadata(null, "document", fileName, docId, null, null, List.of());
        List<Document> chunks = splitAndEnrich(rawDocuments, fileName, metadata);
        vectorStore.add(chunks);
        persistVectorStore();
        DocumentMeta meta = buildMeta(docId, fileName, file.length(), chunks, joinText(rawDocuments));
        documentRegistry.put(docId, meta);
        documentRegistry.put(fileName, meta);
        persistRegistry();
        log.info("本地文档入库完成: {} -> {}个片段", fileName, chunks.size());
        return meta;
    }

    /**
     * 批量索引整个目录(只索引还没入库的文件,根据 fileName 去重)
     */
    public List<DocumentMeta> indexKnowledgeDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("知识库目录不存在: {}", dirPath);
            return Collections.emptyList();
        }

        List<DocumentMeta> results = new ArrayList<>();
        File[] files = dir.listFiles((d, name) -> isSupportedFileName(name));
        if (files == null) {
            return results;
        }

        for (File file : files) {
            try {
                if (documentRegistry.containsKey(file.getName())) {
                    log.debug("跳过已入库文件: {}", file.getName());
                    continue;
                }
                results.add(indexLocalFile(file.getAbsolutePath()));
            } catch (Exception e) {
                log.error("文件入库失败: {} - {}", file.getName(), e.getMessage());
            }
        }
        return results;
    }

    /**
     * 删除整个文档(包括其所有片段向量 + 注册表条目)
     * 支持按 docId 或 fileName 删除(因为注册表两种 key 都存了)
     */
    public boolean deleteDocument(String docIdOrFileName) {
        DocumentMeta meta = documentRegistry.get(docIdOrFileName);
        if (meta == null) {
            return false;
        }
        deleteVectors(meta.getDocumentIds());
        documentRegistry.remove(meta.getDocId());
        documentRegistry.remove(meta.getFileName());
        persistRegistry();
        log.info("文档已删除: {}", docIdOrFileName);
        return true;
    }

    public List<DocumentMeta> listDocuments() {
        return new ArrayList<>(new LinkedHashSet<>(documentRegistry.values()));
    }

    public DocumentMeta getDocumentMeta(String docIdOrFileName) {
        return documentRegistry.get(docIdOrFileName);
    }

    private List<Document> readDocuments(String fileName, byte[] bytes) throws IOException {
        String extension = extension(fileName);
        // 包装成 Resource,Spring AI 的 Reader 都吃 Resource 接口
        Resource resource = new NamedByteArrayResource(bytes, fileName);
        // PDF:按页切分(每页一个 Document),便于后续按页溯源
        if ("pdf".equals(extension)) {
            return new PagePdfDocumentReader(resource).get();
        }
        // DOCX:用 Apache POI 的 XWPFWordExtractor 提取纯文本,整个文件一个 Document
        if ("docx".equals(extension)) {
            return List.of(new Document("raw-" + UUID.randomUUID().toString().replace("-", ""),
                    readDocx(bytes), Map.of("source", fileName)));
        }
        // TXT/MD:走 Spring AI 的 TextReader(按行读)
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("source", fileName);
        return textReader.read();
    }

    /**
     * 提取 DOCX 文本(用 POI)
     * try-with-resources 确保 XWPFDocument 关闭(避免文件句柄泄漏)
     */
    private String readDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 切分 + 元数据增强(RAG 离线索引的核心)
     *
     * 第一阶段:智能切分(Split)
     *   - TokenTextSplitter 按 token 切分(不是按字符)
     *   - chunkSize=800:每个片段 ≤ 800 token,匹配 Embedding 模型容量
     *   - keepSeparator=true:切分时保留段落分隔符,防止句子被拦腰截断
     *
     * 第二阶段:元数据增强(Enrich)
     *   - 合并原始业务元数据(userId/category/tags...)
     *   - 兜底生成片段 ID(保证幂等)
     *   - 写入溯源 + 定位字段(便于检索后展示)
     *   - 写入 enabled=true 软删除开关
     */
    private List<Document> splitAndEnrich(List<Document> documents, String sourceName, Map<String, Object> metadata) {
        //1. 第一阶段：智能切分 (Split)
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withKeepSeparator(true)
                .build();
        // 执行切分操作，将一个长文档变成多个短文档列表。
        List<Document> chunks = splitter.apply(documents);
        // todo
        //2. 第二阶段：元数据增强 (Enrich)
        List<Document> enriched = new ArrayList<>();
        int total = chunks.size();
        for (int i = 0; i < total; i++) {
            Document chunk = chunks.get(i);
            // LinkedHashMap 保留字段顺序,便于调试和日志
            Map<String, Object> chunkMetadata = new LinkedHashMap<>();
            //先放原始笔记信息(userId/category/tags...);后放 splitter 自带元数据(后者覆盖前者)
            chunkMetadata.putAll(metadata);
            chunkMetadata.putAll(chunk.getMetadata());
            // ID 幂等策略: splitter 给了 ID 就沿用,否则兜底 UUID
            // 这样做的好处:同一篇文档重复入库不会产生重复向量(ID 相同会覆盖)
            String chunkId = nonBlank(chunk.getId(), "chunk-" + UUID.randomUUID().toString().replace("-", ""));
            //告诉 AI 这个片段出自哪篇文章或哪个笔记。
            chunkMetadata.put("source", sourceName);
            chunkMetadata.put("sourceName", sourceName);
            chunkMetadata.put("chunkId", chunkId);
            chunkMetadata.put("chunkIndex", i);
            // 双写命名:驼峰 + 下划线同时存,兼容不同向量库的字段命名约定
            chunkMetadata.put("chunk_index", i);
            chunkMetadata.put("totalChunks", total);
            chunkMetadata.put("total_chunks", total);
            // 软删除开关:enabled=false 时检索时过滤掉,避免物理删除
            chunkMetadata.put("enabled", true);
            enriched.add(new Document(chunkId, chunk.getText(), chunkMetadata));
        }
        return enriched;
    }

    /**
     * 构造所有片段都会继承的"基础元数据"
     * 这些字段是检索时过滤/溯源的关键:
     * - userId: 多租户隔离
     * - docId / noteId: 精确定位来源
     * - category / tags: 按分类/标签过滤
     * - enabled: 软删除开关
     */
    private Map<String, Object> baseMetadata(String userId, String sourceType, String sourceName, String docId,
                                             String noteId, String category, List<String> tags) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        // userId 兜底为 "system"(管理员批量导入时用,后续检索会过滤掉)
        metadata.put("userId", nonBlank(userId, "system"));
        metadata.put("docId", docId);
        if (noteId != null && !noteId.isBlank()) {
            metadata.put("noteId", noteId);
        }
        metadata.put("sourceType", sourceType == null || sourceType.isBlank() ? "document" : sourceType);
        metadata.put("sourceName", sourceName);
        metadata.put("title", sourceName);
        metadata.put("category", category == null ? "" : category);
        metadata.put("tags", tags == null ? List.of() : tags);
        metadata.put("createdAt", LocalDateTime.now().toString());
        metadata.put("enabled", true);
        return metadata;
    }

    private DocumentMeta buildMeta(String docId, String fileName, long fileSize, List<Document> chunks, String textContent) {
        DocumentMeta meta = new DocumentMeta();
        meta.setDocId(docId);
        meta.setFileName(fileName);
        meta.setFileSize(fileSize);
        meta.setChunkCount(chunks.size());
        meta.setIndexTime(new Date());
        // 存所有片段的 ID,用于后续按 docId 删除所有片段
        meta.setDocumentIds(chunks.stream().map(Document::getId).toList());
        meta.setTextContent(textContent);
        return meta;
    }

    /**
     * SimpleVectorStore 显式持久化到 JSON 文件
     * Qdrant 不需要此步(其自身有持久化机制),走 if 分支
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
     * 校验文件类型(只支持 txt/md/pdf/docx)
     * 不支持其他类型,避免解析器抛异常
     */
    private void validateFileType(String fileName) {
        if (!isSupportedFileName(fileName)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 .txt、.md、.pdf 和 .docx。文件名: " + fileName);
        }
    }

    private boolean isSupportedFileName(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".pdf") || lower.endsWith(".docx");
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 清理文件名(去掉路径,只保留纯文件名)
     * 防止用户上传 "..\\..\\xxx.pdf" 这种路径攻击
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        return Paths.get(fileName).getFileName().toString();
    }

    private String safeSourceName(String sourceName) {
        return sourceName == null || sourceName.isBlank() ? "未命名学习笔记" : sourceName;
    }

    /**
     * 把 Document 列表拼成单个字符串(用于存储原始文本)
     * PDF 模式时:每个 Document 是一页,用 \n\n 分隔
     */
    private String joinText(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }
        return documents.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    /**
     * 把笔记的"标题/摘要/正文"拼成 Embedding 模型能理解的格式
     * 用中文标签前缀告诉 LLM 每个部分是什么角色(有助于后续语义理解)
     */
    private String noteText(KnowledgeNoteDocument note) {
        return ("标题：" + nonBlank(note.getTitle(), "未命名学习笔记") + "\n\n"
                + "摘要：" + nonBlank(note.getSummary(), "") + "\n\n"
                + "正文：\n" + nonBlank(note.getContent(), "")).trim();
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String asString(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    /**
     * 自定义 ByteArrayResource,覆写 getFilename()
     * Spring AI 的 TextReader 读 Resource 时需要拿到 filename 推断编码
     */
    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String fileName;

        NamedByteArrayResource(byte[] byteArray, String fileName) {
            super(byteArray);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }

    /**
     * 文档元信息(注册表存储用)
     * 包含 docId/fileName/所有片段 ID,用于"按 docId 删除所有片段"
     */
    @lombok.Data
    public static class DocumentMeta {
        private String docId;
        private String fileName;
        private long fileSize;
        private int chunkCount;
        private Date indexTime;
        private List<String> documentIds;
        private String textContent;
    }
}
