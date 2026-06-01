package com.chat.myAgent.agent;

import com.chat.myAgent.model.entity.BookInfo;
import com.chat.myAgent.model.entity.SentimentResult;
import com.chat.myAgent.model.entity.TaskAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 结构化输出 Agent
 *
 * 核心能力：让大模型返回固定格式的 JSON，自动映射为 Java 实体类
 *
 * Spring AI 结构化输出原理：
 * 1. 通过 BeanOutputConverter 分析目标实体类的字段和注解
 * 2. 自动生成 JSON Schema 格式说明，注入到 Prompt 中
 * 3. 大模型按照 Schema 生成 JSON 响应
 * 4. Spring AI 自动反序列化为 Java 对象
 *
 * 使用场景：
 * - 从非结构化文本中提取结构化信息
 * - 让 AI 的输出可以被程序直接处理
 * - 为后续 Agent 的任务规划提供结构化数据基础
 */
@Slf4j
@Component
public class StructuredAgent {

    private final ChatClient baseChatClient;

    @Value("classpath:prompts/structured-book.st")
    private Resource bookPromptResource;

    @Value("classpath:prompts/structured-task.st")
    private Resource taskPromptResource;

    public StructuredAgent(@Qualifier("baseChatClient") ChatClient baseChatClient) {
        this.baseChatClient = baseChatClient;
    }

    /**
     * 提取图书信息（结构化输出示例1）
     *
     * 用户输入："推荐一本Java编程的入门书籍"
     * AI 返回：BookInfo 实体对象（包含书名、作者、简介等字段）
     */
    public BookInfo extractBookInfo(String userMessage) {
        log.debug("StructuredAgent 提取图书信息: {}", userMessage);

        BookInfo result = baseChatClient.prompt()
                .system(bookPromptResource)
                .user(userMessage)
                .call()
                .entity(BookInfo.class);

        log.debug("提取结果: {}", result);
        return result;
    }

    /**
     * 任务分析（结构化输出示例2）
     *
     * 用户输入任意需求描述，AI 返回结构化的任务分析结果
     * 这个能力是阶段5「任务规划Agent」的基础
     */
    public TaskAnalysis analyzeTask(String userMessage) {
        log.debug("StructuredAgent 分析任务: {}", userMessage);

        TaskAnalysis result = baseChatClient.prompt()
                .system(taskPromptResource)
                .user(userMessage)
                .call()
                .entity(TaskAnalysis.class);

        log.debug("分析结果: {}", result);
        return result;
    }

    /**
     * 情感分析（结构化输出示例3）
     *
     * 对用户输入的文本进行情感分析，返回情感倾向和置信度
     */
    public SentimentResult analyzeSentiment(String text) {
        log.debug("StructuredAgent 情感分析: {}", text);

        SentimentResult result = baseChatClient.prompt()
                .system("你是一个情感分析专家。分析用户输入文本的情感倾向。" +
                        "严格按照指定 JSON 格式返回结果。")
                .user(text)
                .call()
                .entity(SentimentResult.class);

        log.debug("情感分析结果: {}", result);
        return result;
    }
}
