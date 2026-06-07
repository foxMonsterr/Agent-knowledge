package com.chat.myAgent.learn.tool;

import com.chat.myAgent.learn.service.websearch.WebHit;
import com.chat.myAgent.learn.service.websearch.WebSearchProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSearchTool {

    private final WebSearchProvider webSearchProvider;
    private final ObjectMapper objectMapper;

    @Tool(description = "联网搜索最新信息。当本地知识库无法回答用户问题时调用。")
    public String searchWeb(@ToolParam(description = "搜索查询关键词") String query,
                            @ToolParam(description = "返回结果数量") int topK) {
        List<WebHit> hits = webSearchProvider.search(query, Math.min(topK, 10));
        if (hits.isEmpty()) {
            return toJson(Map.of("empty", true, "message", "未搜索到相关结果。"));
        }
        return toJson(Map.of("hits", hits, "totalHits", hits.size()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"搜索结果序列化失败\"}";
        }
    }
}
