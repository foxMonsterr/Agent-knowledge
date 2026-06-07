package com.chat.myAgent.learn.react;

import com.chat.myAgent.learn.service.LearnUserService;
import com.chat.myAgent.learn.tool.KnowledgeTool;
import com.chat.myAgent.rag.RetrievalService;
import com.chat.myAgent.react.core.ReActRunRequest;
import com.chat.myAgent.react.core.ReActTool;
import com.chat.myAgent.react.core.ReActToolResult;
import com.chat.myAgent.react.model.ReActTraceDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchNotesReActTool implements ReActTool {

    private final KnowledgeTool knowledgeTool;
    private final RetrievalService retrievalService;
    private final LearnUserService learnUserService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "search_notes";
    }

    @Override
    public String displayName() {
        return "检索个人知识库";
    }

    @Override
    public String category() {
        return "knowledge";
    }

    @Override
    public boolean supports(ReActRunRequest request) {
        return true;
    }

    @Override
    public Map<String, Object> input(ReActRunRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("query", request.getMessage());
        input.put("topK", 5);
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            input.put("tags", request.getTags());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            input.put("category", request.getCategory());
        }
        if (request.getNoteIds() != null && !request.getNoteIds().isEmpty()) {
            input.put("noteIds", request.getNoteIds());
        }
        return input;
    }

    @Override
    public ReActToolResult execute(ReActRunRequest request, Map<String, Object> input) {
        List<ReActTraceDocument.ReActSourceRefDocument> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        String userId = learnUserService.currentUserId();
        String query = request.getMessage();
        int topK = input.get("topK") instanceof Number n ? n.intValue() : 5;

        // 1. 向量检索优先
        try {
            List<Document> docs = retrievalService.retrieveForUser(userId, query, topK, 0.5);
            if (!docs.isEmpty()) {
                for (Document doc : docs) {
                    String source = String.valueOf(doc.getMetadata().getOrDefault("title",
                            doc.getMetadata().getOrDefault("source", "导入资料")));
                    String chunkId = doc.getId();
                    String text = doc.getText();
                    String sourceType = String.valueOf(doc.getMetadata().getOrDefault("sourceType", "chunk"));
                    String noteId = asString(doc.getMetadata().get("noteId"));
                    String docId = asString(doc.getMetadata().get("docId"));
                    sources.add(ReActTraceDocument.ReActSourceRefDocument.builder()
                            .sourceId(chunkId == null ? source : chunkId)
                            .sourceType("note".equals(sourceType) ? "note" : "chunk")
                            .title(source)
                            .snippet(abbreviate(text, 220))
                            .noteId(noteId)
                            .docId(docId)
                            .chunkId(chunkId)
                            .build());
                    context.append("来源: ").append(source).append("\n").append(abbreviate(text, 500)).append("\n\n");
                }
            }
        } catch (Exception e) {
            log.warn("向量检索失败: {}", e.getMessage());
        }

        // 2. 向量无结果时回退到笔记关键词检索
        if (sources.isEmpty()) {
            try {
                String json = knowledgeTool.searchNotes(query, topK);
                List<Map<String, Object>> results = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> result : results) {
                    String noteId = asString(result.get("noteId"));
                    String title = asString(result.get("title"));
                    String snippet = asString(result.get("snippet"));
                    sources.add(ReActTraceDocument.ReActSourceRefDocument.builder()
                            .sourceId(noteId)
                            .sourceType("note")
                            .title(title)
                            .snippet(snippet)
                            .noteId(noteId)
                            .score(asDouble(result.get("score")))
                            .build());
                    context.append("来源: ").append(title).append("\n").append(snippet).append("\n\n");
                }
            } catch (Exception e) {
                log.warn("笔记检索失败: {}", e.getMessage());
            }
        }

        String observation = sources.isEmpty()
                ? "未检索到相关来源。"
                : "检索到 " + sources.size() + " 条相关来源。";
        return ReActToolResult.builder()
                .observation(observation)
                .context(context.toString())
                .sources(sources)
                .build();
    }

    private String abbreviate(String text, int max) {
        if (text == null) return "";
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= max ? compact : compact.substring(0, max) + "...";
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        return null;
    }
}
