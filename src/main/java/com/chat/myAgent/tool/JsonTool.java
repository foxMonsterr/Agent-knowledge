package com.chat.myAgent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class JsonTool {

    private static final int MAX_JSON_LENGTH = 50_000;

    private final ObjectMapper objectMapper;

    public JsonTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Tool(description = "校验 JSON 字符串是否合法")
    public String validateJson(@ToolParam(description = "JSON 字符串") String json) {
        try {
            objectMapper.readTree(limit(json));
            return "JSON 合法。";
        } catch (Exception e) {
            return "JSON 不合法: " + e.getMessage();
        }
    }

    @Tool(description = "格式化 JSON 字符串")
    public String formatJson(@ToolParam(description = "JSON 字符串") String json) {
        try {
            JsonNode node = objectMapper.readTree(limit(json));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            return "JSON 格式化失败: " + e.getMessage();
        }
    }

    @Tool(description = "压缩 JSON 字符串，去除不必要空白")
    public String minifyJson(@ToolParam(description = "JSON 字符串") String json) {
        try {
            JsonNode node = objectMapper.readTree(limit(json));
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "JSON 压缩失败: " + e.getMessage();
        }
    }

    @Tool(description = "从 JSON 中按简单 dot path 提取字段，例如 user.name 或 items.0.title")
    public String extractJsonField(@ToolParam(description = "JSON 字符串") String json,
                                   @ToolParam(description = "字段路径，例如 user.name 或 items.0.title") String fieldPath) {
        try {
            JsonNode current = objectMapper.readTree(limit(json));
            if (fieldPath == null || fieldPath.isBlank()) {
                return objectMapper.writeValueAsString(current);
            }
            Iterator<String> parts = java.util.Arrays.stream(fieldPath.split("\\.")).iterator();
            while (parts.hasNext() && current != null) {
                String part = parts.next();
                if (current.isArray() && part.matches("\\d+")) {
                    current = current.get(Integer.parseInt(part));
                } else {
                    current = current.get(part);
                }
            }
            if (current == null || current.isMissingNode()) {
                return "未找到字段: " + fieldPath;
            }
            return current.isValueNode() ? current.asText() : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(current);
        } catch (Exception e) {
            return "字段提取失败: " + e.getMessage();
        }
    }

    private String limit(String json) {
        if (json == null) {
            return "";
        }
        return json.length() <= MAX_JSON_LENGTH ? json : json.substring(0, MAX_JSON_LENGTH);
    }
}
