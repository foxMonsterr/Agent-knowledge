package com.chat.myAgent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RegexTool {

    private static final int MAX_TEXT_LENGTH = 10_000;
    private static final int MAX_REGEX_LENGTH = 200;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\\u4e00-\\u9fff]+");

    @Tool(description = "使用正则表达式查找文本中的匹配项")
    public String findMatches(@ToolParam(description = "待匹配文本") String text,
                              @ToolParam(description = "正则表达式") String regex) {
        try {
            Pattern pattern = Pattern.compile(limitRegex(regex));
            String result = pattern.matcher(limitText(text)).results()
                    .limit(50)
                    .map(match -> match.group())
                    .collect(Collectors.joining("\n"));
            return result.isBlank() ? "未找到匹配项。" : "匹配结果：\n" + result;
        } catch (Exception e) {
            return "正则匹配失败: " + e.getMessage();
        }
    }

    @Tool(description = "使用正则表达式替换文本中的匹配项")
    public String replaceMatches(@ToolParam(description = "原始文本") String text,
                                 @ToolParam(description = "正则表达式") String regex,
                                 @ToolParam(description = "替换内容") String replacement) {
        try {
            return Pattern.compile(limitRegex(regex)).matcher(limitText(text)).replaceAll(replacement == null ? "" : replacement);
        } catch (Exception e) {
            return "正则替换失败: " + e.getMessage();
        }
    }

    @Tool(description = "从文本中提取邮箱地址")
    public String extractEmails(@ToolParam(description = "待提取文本") String text) {
        String result = EMAIL_PATTERN.matcher(limitText(text)).results()
                .limit(50)
                .map(match -> match.group())
                .distinct()
                .collect(Collectors.joining("\n"));
        return result.isBlank() ? "未找到邮箱地址。" : "邮箱地址：\n" + result;
    }

    @Tool(description = "从文本中提取 URL 链接")
    public String extractUrls(@ToolParam(description = "待提取文本") String text) {
        String result = URL_PATTERN.matcher(limitText(text)).results()
                .limit(50)
                .map(match -> match.group())
                .distinct()
                .collect(Collectors.joining("\n"));
        return result.isBlank() ? "未找到 URL。" : "URL：\n" + result;
    }

    private String limitText(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }

    private String limitRegex(String regex) {
        if (regex == null) {
            return "";
        }
        return regex.length() <= MAX_REGEX_LENGTH ? regex : regex.substring(0, MAX_REGEX_LENGTH);
    }
}
