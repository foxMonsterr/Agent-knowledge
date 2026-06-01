package com.chat.myAgent.tool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 翻译工具
 *
 * 利用大模型自身的多语言能力实现翻译
 * 这是一个"AI调用AI"的经典模式——主Agent调用翻译工具，翻译工具内部再调用模型
 *
 */
@Component
public class TranslateTool {

    private final ChatClient baseChatClient;

    public TranslateTool(@Qualifier("baseChatClient") ChatClient baseChatClient) {
        this.baseChatClient = baseChatClient;
    }

    /**
     * 文本翻译
     */
    @Tool(description = "将文本翻译为指定语言。支持的目标语言：中文(chinese)、英文(english)、日文(japanese)、韩文(korean)、法文(french)、德文(german)")
    public String translate(
            @ToolParam(description = "要翻译的文本内容") String text,
            @ToolParam(description = "目标语言，如：chinese、english、japanese、korean、french、german") String targetLanguage) {
        try {
            String prompt = String.format(
                    "请将以下文本翻译为%s，只返回翻译结果，不要添加任何解释：\n\n%s",
                    mapLanguageName(targetLanguage), text);

            String result = baseChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return "翻译结果（" + mapLanguageName(targetLanguage) + "）：\n" + result;
        } catch (Exception e) {
            return "翻译失败: " + e.getMessage();
        }
    }

    /**
     * 语言检测
     */
    @Tool(description = "检测一段文本是什么语言")
    public String detectLanguage(
            @ToolParam(description = "要检测语言的文本") String text) {
        try {
            String prompt = "请判断以下文本是什么语言，只回答语言名称（如：中文、英文、日文等）：\n\n" + text;

            String result = baseChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return "检测结果：该文本的语言为 " + result;
        } catch (Exception e) {
            return "语言检测失败: " + e.getMessage();
        }
    }

    /**
     * 将英文语言标识映射为中文名称
     */
    private String mapLanguageName(String language) {
        return switch (language.toLowerCase()) {
            case "chinese", "zh", "中文" -> "中文";
            case "english", "en", "英文" -> "英文";
            case "japanese", "ja", "日文" -> "日文";
            case "korean", "ko", "韩文" -> "韩文";
            case "french", "fr", "法文" -> "法文";
            case "german", "de", "德文" -> "德文";
            default -> language;
        };
    }
}
