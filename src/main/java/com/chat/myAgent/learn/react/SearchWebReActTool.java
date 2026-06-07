package com.chat.myAgent.learn.react;

import com.chat.myAgent.learn.tool.WebSearchTool;
import com.chat.myAgent.react.core.ReActRunRequest;
import com.chat.myAgent.react.core.ReActTool;
import com.chat.myAgent.react.core.ReActToolResult;
import com.chat.myAgent.react.model.ReActTraceDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchWebReActTool implements ReActTool {

    private final WebSearchTool webSearchTool;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "search_web";
    }

    @Override
    public String displayName() {
        return "联网搜索";
    }

    @Override
    public String category() {
        return "web";
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
        return input;
    }

    @Override
    public ReActToolResult execute(ReActRunRequest request, Map<String, Object> input) {
        String query = input.get("query") instanceof String s ? s : request.getMessage();
        int topK = input.get("topK") instanceof Number n ? n.intValue() : 5;

        try {
            String json = webSearchTool.searchWeb(query, topK);
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            boolean empty = Boolean.TRUE.equals(result.get("empty"));

            if (empty) {
                return ReActToolResult.builder()
                        .observation("联网搜索未返回相关结果。")
                        .build();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hits = (List<Map<String, Object>>) result.get("hits");
            List<ReActTraceDocument.ReActSourceRefDocument> sources = new ArrayList<>();
            StringBuilder context = new StringBuilder();
            if (hits != null) {
                for (Map<String, Object> hit : hits) {
                    String title = asString(hit.get("title"));
                    String url = asString(hit.get("url"));
                    String snippet = asString(hit.get("snippet"));
                    sources.add(ReActTraceDocument.ReActSourceRefDocument.builder()
                            .sourceId(url == null ? title : url)
                            .sourceType("web")
                            .title(title)
                            .snippet(abbreviate(snippet, 220))
                            .build());
                    context.append("来源: ").append(title).append(" (").append(url).append(")\n").append(snippet).append("\n\n");
                }
            }

            return ReActToolResult.builder()
                    .observation("联网搜索返回 " + (hits == null ? 0 : hits.size()) + " 条结果。")
                    .context(context.toString())
                    .sources(sources)
                    .build();
        } catch (Exception e) {
            log.warn("联网搜索失败: {}", e.getMessage());
            return ReActToolResult.builder()
                    .observation("联网搜索失败: " + e.getMessage())
                    .build();
        }
    }

    private String abbreviate(String text, int max) {
        if (text == null) return "";
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= max ? compact : compact.substring(0, max) + "...";
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
