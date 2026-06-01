package com.chat.myAgent.controller;

import com.chat.myAgent.agent.RagAgent;
import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.dto.KnowledgeRequest;
import com.chat.myAgent.model.vo.DocumentVO;
import com.chat.myAgent.model.vo.KnowledgeResponse;
import com.chat.myAgent.rag.DocumentService;
import com.chat.myAgent.rag.DocumentService.DocumentMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库管理 + RAG 问答接口
 *
 * 功能分类：
 * 1. 文档管理：上传、列表、删除
 * 2. RAG问答：基于知识库回答问题
 * 3. 调试工具：纯检索（不生成回答）
 */
@Tag(name = "知识库管理", description = "文档上传/加载/删除 + RAG知识库问答 + 检索调试")
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final DocumentService documentService;
    private final RagAgent ragAgent;

    // ==================== 文档管理接口 ====================

    /**
     * 上传文档并入库
     * POST /api/v1/knowledge/upload
     *
     * 支持 .txt 和 .md 格式
     * 文件上传后自动进行切片、向量化、存储
     */
    @Operation(summary = "上传文档并入库", description = "支持.txt和.md格式，文件上传后自动切片、向量化、存储")
    @PostMapping("/upload")
    public R<DocumentVO> uploadDocument(@Parameter(description = "上传的文件（.txt/.md）", required = true) @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return R.paramError("文件不能为空");
            }

            DocumentMeta meta = documentService.uploadAndIndex(file);

            DocumentVO vo = DocumentVO.builder()
                    .fileName(meta.getFileName())
                    .fileSize(meta.getFileSize())
                    .fileSizeReadable(formatFileSize(meta.getFileSize()))
                    .chunkCount(meta.getChunkCount())
                    .indexTime(meta.getIndexTime())
                    .build();

            return R.ok("文档上传并入库成功", vo);
        } catch (IllegalArgumentException e) {
            return R.paramError(e.getMessage());
        } catch (Exception e) {
            log.error("文档上传失败", e);
            return R.fail("文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量加载知识库目录
     * POST /api/v1/knowledge/load-directory
     *
     * 将指定目录下的所有 txt/md 文件批量入库
     * 默认目录：src/main/resources/knowledge/
     */
    @Operation(summary = "批量加载知识库目录", description = "将指定目录下的所有txt/md文件批量入库")
    @PostMapping("/load-directory")
    public R<List<DocumentVO>> loadDirectory(
            @Parameter(description = "知识库目录路径") @RequestParam(value = "path", defaultValue = "src/main/resources/knowledge/") String dirPath) {
        List<DocumentMeta> metas = documentService.indexKnowledgeDirectory(dirPath);

        List<DocumentVO> vos = metas.stream()
                .map(meta -> DocumentVO.builder()
                        .fileName(meta.getFileName())
                        .fileSize(meta.getFileSize())
                        .fileSizeReadable(formatFileSize(meta.getFileSize()))
                        .chunkCount(meta.getChunkCount())
                        .indexTime(meta.getIndexTime())
                        .build())
                .collect(Collectors.toList());

        return R.ok("批量入库完成，共处理 " + vos.size() + " 个文件", vos);
    }

    /**
     * 获取已入库文档列表
     * GET /api/v1/knowledge/documents
     */
    @Operation(summary = "获取已入库文档列表")
    @GetMapping("/documents")
    public R<List<DocumentVO>> listDocuments() {
        List<DocumentMeta> metas = documentService.listDocuments();

        List<DocumentVO> vos = metas.stream()
                .map(meta -> DocumentVO.builder()
                        .fileName(meta.getFileName())
                        .fileSize(meta.getFileSize())
                        .fileSizeReadable(formatFileSize(meta.getFileSize()))
                        .chunkCount(meta.getChunkCount())
                        .indexTime(meta.getIndexTime())
                        .build())
                .collect(Collectors.toList());

        return R.ok(vos);
    }

    /**
     * 删除指定文档
     * DELETE /api/v1/knowledge/documents/{fileName}
     */
    @Operation(summary = "删除指定文档")
    @DeleteMapping("/documents/{fileName}")
    public R<String> deleteDocument(@PathVariable String fileName) {
        if (fileName == null || !fileName.matches("[a-zA-Z0-9_.\\u4e00-\\u9fa5\\s-]+")) {
            return R.fail(400, "非法文件名");
        }
        boolean success = documentService.deleteDocument(fileName);
        if (success) {
            return R.ok("文档已删除");
        } else {
            return R.fail(400, "操作失败");
        }
    }

    // ==================== RAG 问答接口 ====================

    /**
     * 知识库问答（自动RAG模式）
     * POST /api/v1/knowledge/ask
     *
     * 使用 QuestionAnswerAdvisor 自动完成检索和回答
     * 支持多轮追问（同一 conversationId）
     *
     * 请求示例：
     * {
     *   "question": "公司的年假制度是怎样的？",
     *   "conversationId": "rag-001"
     * }
     */
    @Operation(summary = "知识库问答（自动RAG）", description = "使用QuestionAnswerAdvisor自动完成检索和回答，支持多轮追问")
    @PostMapping("/ask")
    public R<KnowledgeResponse> ask(@Valid @RequestBody KnowledgeRequest request) {
        KnowledgeResponse response = ragAgent.ask(
                request.getQuestion(),
                request.getConversationId()
        );
        return R.ok(response);
    }

    /**
     * 知识库问答（手动RAG模式）
     * POST /api/v1/knowledge/ask/manual
     *
     * 手动控制检索和拼接过程，可以看到更清晰的RAG流程
     */
    @Operation(summary = "知识库问答（手动RAG）", description = "手动控制检索和拼接过程，可以看到更清晰的RAG流程")
    @PostMapping("/ask/manual")
    public R<KnowledgeResponse> askManual(@Valid @RequestBody KnowledgeRequest request) {
        KnowledgeResponse response = ragAgent.askManual(
                request.getQuestion(),
                request.getConversationId()
        );
        return R.ok(response);
    }

    // ==================== RAG 流式问答 ====================

    /**
     * 知识库流式问答（SSE）
     * GET /api/v1/knowledge/stream?question=xxx&conversationId=xxx&manual=false
     */
    @Operation(summary = "知识库流式问答（SSE）", description = "流式返回基于知识库的回答，支持手动/自动RAG模式")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(
            @Parameter(description = "问题", required = true) @RequestParam("message") String message,
            @Parameter(description = "会话ID") @RequestParam(required = false) String conversationId,
            @Parameter(description = "是否手动模式") @RequestParam(defaultValue = "false") boolean manual) {
        return ragAgent.askStream(message, conversationId, manual);
    }

    // ==================== 调试接口 ====================

    /**
     * 纯检索（不生成回答，只返回检索到的片段）
     * GET /api/v1/knowledge/search?query=xxx
     *
     * 用于调试：查看某个问题能检索到哪些文档片段
     */
    @Operation(summary = "纯检索（调试用）", description = "不生成回答，只返回检索到的文档片段，用于调试检索效果")
    @GetMapping("/search")
    public R<String> search(@Parameter(description = "检索关键词", required = true) @RequestParam String query) {
        String result = ragAgent.searchOnly(query);
        return R.ok(result);
    }

    /**
     * 知识库状态
     * GET /api/v1/knowledge/status
     */
    @Operation(summary = "知识库状态")
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        List<DocumentMeta> docs = documentService.listDocuments();
        int totalChunks = docs.stream().mapToInt(DocumentMeta::getChunkCount).sum();

        Map<String, Object> statusMap = Map.of(
                "documentCount", docs.size(),
                "totalChunks", totalChunks,
                "documents", docs.stream().map(DocumentMeta::getFileName).toList()
        );

        return R.ok(statusMap);
    }

    // ==================== 工具方法 ====================

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
