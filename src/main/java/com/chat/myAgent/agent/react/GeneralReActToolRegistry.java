package com.chat.myAgent.agent.react;

import com.chat.myAgent.rag.RetrievalService;
import com.chat.myAgent.react.core.ReActRunRequest;
import com.chat.myAgent.react.core.ReActTool;
import com.chat.myAgent.react.core.ReActToolResult;
import com.chat.myAgent.react.model.ReActTraceDocument;
import com.chat.myAgent.tool.CalculatorTool;
import com.chat.myAgent.tool.CollectionTool;
import com.chat.myAgent.tool.DateTimeTool;
import com.chat.myAgent.tool.DbQueryTool;
import com.chat.myAgent.tool.DocParseTool;
import com.chat.myAgent.tool.JsonTool;
import com.chat.myAgent.tool.RegexTool;
import com.chat.myAgent.tool.SystemInfoTool;
import com.chat.myAgent.tool.TextTool;
import com.chat.myAgent.tool.TranslateTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class GeneralReActToolRegistry {

    private final DateTimeTool dateTimeTool;
    private final CalculatorTool calculatorTool;
    private final TranslateTool translateTool;
    private final DocParseTool docParseTool;
    private final DbQueryTool dbQueryTool;
    private final TextTool textTool;
    private final JsonTool jsonTool;
    private final CollectionTool collectionTool;
    private final RegexTool regexTool;
    private final SystemInfoTool systemInfoTool;
    private final RetrievalService retrievalService;

    public List<ReActTool> selectTools(ReActRunRequest request) {
        List<ReActTool> all = allTools();
        List<String> requested = normalizeToolNames(request.getToolNames());
        if (!requested.isEmpty()) {
            List<ReActTool> selected = all.stream()
                    .filter(tool -> requested.contains(tool.name()) || requested.contains(alias(tool.name())))
                    .toList();
            return selected.isEmpty() ? List.of(systemTool()) : selected;
        }
        return all;
    }

    private List<ReActTool> allTools() {
        return List.of(
                dateTime(),
                calculator(),
                translate(),
                doc(),
                db(),
                text(),
                json(),
                collection(),
                regex(),
                knowledgeSearch(),
                systemTool()
        );
    }

    private ReActTool dateTime() {
        return simple("datetime", "日期时间工具", "utility",
                request -> containsAny(request.getMessage(), "时间", "日期", "今天", "明天", "星期", "几天"),
                request -> Map.of("query", request.getMessage()),
                (request, input) -> {
                    String message = request.getMessage();
                    if (containsAny(message, "明天")) {
                        return dateTimeTool.addDays(1);
                    }
                    if (containsAny(message, "星期", "周几")) {
                        return dateTimeTool.getCurrentDayOfWeek();
                    }
                    return dateTimeTool.getCurrentDateTime();
                });
    }

    private ReActTool calculator() {
        return simple("calculator", "计算器", "utility",
                request -> expression(request.getMessage()) != null || containsAny(request.getMessage(), "计算", "百分比", "换算"),
                request -> Map.of("expression", expression(request.getMessage()) == null ? request.getMessage() : expression(request.getMessage())),
                (request, input) -> calculatorTool.calculate(String.valueOf(input.get("expression"))));
    }

    private ReActTool translate() {
        return simple("translate", "翻译工具", "utility",
                request -> containsAny(request.getMessage(), "翻译", "translate"),
                request -> Map.of("text", request.getMessage(), "targetLanguage", "中文"),
                (request, input) -> translateTool.translate(request.getMessage(), "中文"));
    }

    private ReActTool doc() {
        return simple("doc", "文档读取工具", "utility",
                request -> containsAny(request.getMessage(), "文件", "文档", ".md", ".txt"),
                request -> Map.of("fileName", extractFileName(request.getMessage())),
                (request, input) -> {
                    String fileName = String.valueOf(input.get("fileName"));
                    if (fileName == null || fileName.isBlank() || "null".equals(fileName)) {
                        return docParseTool.listFiles();
                    }
                    return docParseTool.readFile(fileName);
                });
    }

    private ReActTool db() {
        return simple("db", "模拟数据库工具", "utility",
                request -> containsAny(request.getMessage(), "员工", "部门", "薪资", "数据库"),
                request -> Map.of("query", request.getMessage()),
                (request, input) -> {
                    if (containsAny(request.getMessage(), "部门")) {
                        return dbQueryTool.queryDepartments();
                    }
                    if (containsAny(request.getMessage(), "平均", "统计")) {
                        return dbQueryTool.statistics("avg_salary");
                    }
                    return dbQueryTool.queryEmployees("", "");
                });
    }

    private ReActTool text() {
        return simple("text", "文本处理工具", "utility",
                request -> containsAny(request.getMessage(), "文本", "字符", "词频", "字数", "行数"),
                request -> Map.of("text", request.getMessage()),
                (request, input) -> textTool.textStats(request.getMessage()));
    }

    private ReActTool json() {
        return simple("json", "JSON 工具", "utility",
                request -> containsAny(request.getMessage(), "json", "JSON", "{", "["),
                request -> Map.of("json", extractJson(request.getMessage())),
                (request, input) -> jsonTool.formatJson(String.valueOf(input.get("json"))));
    }

    private ReActTool collection() {
        return simple("collection", "列表统计工具", "utility",
                request -> numberList(request.getMessage()) != null || containsAny(request.getMessage(), "列表", "去重", "排序", "统计"),
                request -> Map.of("items", numberList(request.getMessage()) == null ? request.getMessage() : numberList(request.getMessage())),
                (request, input) -> collectionTool.numberStats(String.valueOf(input.get("items"))));
    }

    private ReActTool regex() {
        return simple("regex", "正则提取工具", "utility",
                request -> containsAny(request.getMessage(), "邮箱", "URL", "url", "正则", "@", "http"),
                request -> Map.of("text", request.getMessage()),
                (request, input) -> {
                    if (containsAny(request.getMessage(), "http", "URL", "url")) {
                        return regexTool.extractUrls(request.getMessage());
                    }
                    return regexTool.extractEmails(request.getMessage());
                });
    }

    private ReActTool knowledgeSearch() {
        return new ReActTool() {
            @Override
            public String name() {
                return "knowledge_search";
            }

            @Override
            public String displayName() {
                return "通用知识库检索";
            }

            @Override
            public String category() {
                return "knowledge";
            }

            @Override
            public boolean supports(ReActRunRequest request) {
                return containsAny(request.getMessage(), "知识库", "资料", "文档中", "检索");
            }

            @Override
            public Map<String, Object> input(ReActRunRequest request) {
                return Map.of("query", request.getMessage(), "topK", 5);
            }

            @Override
            public ReActToolResult execute(ReActRunRequest request, Map<String, Object> input) {
                List<ReActTraceDocument.ReActSourceRefDocument> sources = new ArrayList<>();
                StringBuilder context = new StringBuilder();
                try {
                    List<Document> docs = retrievalService.retrieve(request.getMessage(), 5, 0.5);
                    for (Document doc : docs) {
                        String source = String.valueOf(doc.getMetadata().getOrDefault("source", "知识库资料"));
                        sources.add(ReActTraceDocument.ReActSourceRefDocument.builder()
                                .sourceId(doc.getId() == null ? source : doc.getId())
                                .sourceType("document")
                                .title(source)
                                .snippet(abbreviate(doc.getText(), 220))
                                .chunkId(doc.getId())
                                .build());
                        context.append("来源: ").append(source).append("\n").append(abbreviate(doc.getText(), 500)).append("\n\n");
                    }
                } catch (Exception e) {
                    return ReActToolResult.builder().observation("知识库检索失败: " + e.getMessage()).build();
                }
                return ReActToolResult.builder()
                        .observation(sources.isEmpty() ? "未找到相关知识库资料。" : "检索到 " + sources.size() + " 条知识库资料。")
                        .context(context.toString())
                        .sources(sources)
                        .build();
            }
        };
    }

    private ReActTool systemTool() {
        return simple("system", "能力说明工具", "system",
                request -> containsAny(request.getMessage(), "天气", "联网", "搜索", "HTTP", "工具", "能力", "运行时", "系统"),
                request -> Map.of("capability", request.getMessage()),
                (request, input) -> {
                    String message = request.getMessage();
                    if (containsAny(message, "天气", "联网", "搜索", "HTTP", "http")) {
                        return systemInfoTool.explainUnsupportedCapability(message);
                    }
                    if (containsAny(message, "运行时", "系统")) {
                        return systemInfoTool.getRuntimeInfo();
                    }
                    return systemInfoTool.listToolCapabilities();
                });
    }

    private ReActTool simple(String name, String displayName, String category,
                            java.util.function.Predicate<ReActRunRequest> supports,
                            java.util.function.Function<ReActRunRequest, Map<String, Object>> input,
                            ToolCall call) {
        return new ReActTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String displayName() {
                return displayName;
            }

            @Override
            public String category() {
                return category;
            }

            @Override
            public boolean supports(ReActRunRequest request) {
                return supports.test(request);
            }

            @Override
            public Map<String, Object> input(ReActRunRequest request) {
                return input.apply(request);
            }

            @Override
            public ReActToolResult execute(ReActRunRequest request, Map<String, Object> input) {
                String output = call.execute(request, input);
                return ReActToolResult.builder()
                        .observation(output)
                        .context(displayName + "结果：\n" + output)
                        .build();
            }
        };
    }

    private List<String> normalizeToolNames(List<String> toolNames) {
        if (toolNames == null) {
            return List.of();
        }
        return toolNames.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    private String alias(String name) {
        return switch (name) {
            case "collection" -> "list";
            case "system" -> "capability";
            default -> name;
        };
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String expression(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("[-+*/().%\\d\\s]{3,}").matcher(message);
        while (matcher.find()) {
            String value = matcher.group().trim();
            if (value.matches(".*\\d.*") && value.matches(".*[-+*/%].*")) {
                return value;
            }
        }
        return null;
    }

    private String numberList(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\d+(?:[,.，\\s]+\\d+){2,}").matcher(message);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractFileName(String message) {
        if (message == null) {
            return "";
        }
        Matcher matcher = Pattern.compile("[A-Za-z0-9._-]+\\.(?:txt|md)").matcher(message);
        return matcher.find() ? matcher.group() : "";
    }

    private String extractJson(String message) {
        if (message == null) {
            return "";
        }
        int objectStart = message.indexOf('{');
        int arrayStart = message.indexOf('[');
        int start;
        if (objectStart < 0) {
            start = arrayStart;
        } else if (arrayStart < 0) {
            start = objectStart;
        } else {
            start = Math.min(objectStart, arrayStart);
        }
        return start < 0 ? message : message.substring(start);
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= max ? compact : compact.substring(0, max) + "...";
    }

    private interface ToolCall {
        String execute(ReActRunRequest request, Map<String, Object> input);
    }
}
