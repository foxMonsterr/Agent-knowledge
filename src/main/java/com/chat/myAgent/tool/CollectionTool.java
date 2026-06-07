package com.chat.myAgent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CollectionTool {

    private static final int MAX_ITEMS = 500;

    @Tool(description = "对逗号分隔的数字做统计，返回 count/sum/avg/min/max/median")
    public String numberStats(@ToolParam(description = "逗号分隔数字，例如 1,5,2,5,9") String numbers) {
        List<Double> values = parseNumbers(numbers);
        if (values.isEmpty()) {
            return "没有解析到有效数字。";
        }
        List<Double> sorted = values.stream().sorted().toList();
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double median = sorted.size() % 2 == 1
                ? sorted.get(sorted.size() / 2)
                : (sorted.get(sorted.size() / 2 - 1) + sorted.get(sorted.size() / 2)) / 2;
        return "数字统计：\n"
                + "- count: " + values.size() + "\n"
                + "- sum: " + format(sum) + "\n"
                + "- avg: " + format(sum / values.size()) + "\n"
                + "- min: " + format(sorted.get(0)) + "\n"
                + "- max: " + format(sorted.get(sorted.size() - 1)) + "\n"
                + "- median: " + format(median);
    }

    @Tool(description = "对逗号分隔数字排序，direction 支持 asc 或 desc")
    public String sortNumbers(@ToolParam(description = "逗号分隔数字") String numbers,
                              @ToolParam(description = "排序方向 asc 或 desc") String direction) {
        List<Double> values = new ArrayList<>(parseNumbers(numbers));
        if ("desc".equalsIgnoreCase(direction)) {
            values.sort(Comparator.reverseOrder());
        } else {
            values.sort(Comparator.naturalOrder());
        }
        return values.stream().map(this::format).collect(Collectors.joining(", "));
    }

    @Tool(description = "对逗号或换行分隔的文本列表去重")
    public String deduplicateItems(@ToolParam(description = "文本列表") String items) {
        return parseItems(items).stream().distinct().collect(Collectors.joining("\n"));
    }

    @Tool(description = "统计逗号或换行分隔文本列表中每个项目出现次数")
    public String countItems(@ToolParam(description = "文本列表") String items) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String item : parseItems(items)) {
            counts.merge(item, 1L, Long::sum);
        }
        if (counts.isEmpty()) {
            return "没有解析到有效列表项。";
        }
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n", "列表频次：\n", ""));
    }

    private List<Double> parseNumbers(String numbers) {
        if (numbers == null || numbers.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(numbers.split("[,，\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .limit(MAX_ITEMS)
                .flatMap(item -> {
                    try {
                        return java.util.stream.Stream.of(Double.parseDouble(item));
                    } catch (NumberFormatException e) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();
    }

    private List<String> parseItems(String items) {
        if (items == null || items.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(items.split("[,，\\r\\n]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .limit(MAX_ITEMS)
                .toList();
    }

    private String format(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
