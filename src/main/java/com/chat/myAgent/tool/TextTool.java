package com.chat.myAgent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class TextTool {

    private static final int MAX_TEXT_LENGTH = 20_000;
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");

    @Tool(description = "统计文本信息，包括字符数、行数、单词数、中文字符数")
    public String textStats(@ToolParam(description = "要统计的文本") String text) {
        String safeText = limit(text);
        long chineseCount = CHINESE_PATTERN.matcher(safeText).results().count();
        long lineCount = safeText.isBlank() ? 0 : safeText.lines().count();
        long wordCount = Arrays.stream(safeText.trim().split("\\s+"))
                .filter(item -> !item.isBlank())
                .count();
        return "文本统计：\n"
                + "- 字符数: " + safeText.length() + "\n"
                + "- 行数: " + lineCount + "\n"
                + "- 单词数: " + wordCount + "\n"
                + "- 中文字符数: " + chineseCount;
    }

    @Tool(description = "压缩文本中的连续空白字符，便于清洗文本")
    public String normalizeWhitespace(@ToolParam(description = "要清洗的文本") String text) {
        return limit(text).replaceAll("\\s+", " ").trim();
    }

    @Tool(description = "按指定最大长度截断文本")
    public String truncateText(@ToolParam(description = "原始文本") String text,
                               @ToolParam(description = "最大长度") int maxLength) {
        String safeText = limit(text);
        int safeMax = Math.max(1, Math.min(maxLength, MAX_TEXT_LENGTH));
        if (safeText.length() <= safeMax) {
            return safeText;
        }
        return safeText.substring(0, safeMax) + "...";
    }

    @Tool(description = "提取文本中的高频词，适合快速观察文本主题")
    public String extractTopWords(@ToolParam(description = "要分析的文本") String text,
                                  @ToolParam(description = "返回数量") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 30));
        Map<String, Long> frequency = Arrays.stream(limit(text).toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fff]+"))
                .map(String::trim)
                .filter(word -> word.length() > 1)
                .collect(Collectors.groupingBy(word -> word, LinkedHashMap::new, Collectors.counting()));

        if (frequency.isEmpty()) {
            return "未提取到有效词语。";
        }

        return frequency.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(safeLimit)
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n", "高频词：\n", ""));
    }

    private String limit(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }
}
