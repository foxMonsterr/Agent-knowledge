package com.chat.myAgent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * ChatClient 配置
 *
 * 说明：
 * - 主模型使用 DeepSeek Chat
 * - 兜底模型使用 Qwen3.6-Plus
 * - 两者通过独立的 OpenAiApi + OpenAiChatModel + ChatClient 进行隔离，
 *   这样失败切换时不会复用同一个底层模型配置。
 */
@Configuration
public class ChatClientConfig {

    @Value("${smart-agent.models.default-model:deepseek-chat}")
    private String primaryModelName;

    @Value("${smart-agent.models.fallback-model:qwen3.6-plus}")
    private String fallbackModelName;

    @Value("${deepseek.api-base-url:${spring.ai.openai.base-url:https://api.deepseek.com}}")
    private String deepseekBaseUrl;

    @Value("${deepseek.api-key:${DEEPSEEK_API_KEY:}}")
    private String deepseekApiKey;

    @Value("${qwen.api-base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String qwenBaseUrl;

    @Value("${qwen.api-key:${QWEN_API_KEY:}}")
    private String qwenApiKey;

    @Value("${smart-agent.network.connect-timeout:8s}")
    private Duration connectTimeout;

    @Value("${smart-agent.network.read-timeout:25s}")
    private Duration readTimeout;

    @Value("${smart-agent.retry.primary.max-attempts:1}")
    private int primaryMaxAttempts;

    @Value("${smart-agent.retry.fallback.max-attempts:1}")
    private int fallbackMaxAttempts;

    @Value("${smart-agent.retry.backoff-ms:200}")
    private long retryBackoffMs;

    @Value("classpath:prompts/chat-system.st")
    private Resource chatSystemPrompt;

    @Value("classpath:prompts/rag-system.st")
    private Resource ragSystemPrompt;

    @Value("classpath:prompts/full-agent-system.st")
    private Resource fullAgentSystemPrompt;

    @Bean("primaryOpenAiApi")
    public OpenAiApi primaryOpenAiApi() {
        return OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(deepseekBaseUrl))
                .apiKey(deepseekApiKey)
                .restClientBuilder(buildRestClientBuilder())
                .build();
    }

    @Bean("fallbackOpenAiApi")
    public OpenAiApi fallbackOpenAiApi() {
        return OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(qwenBaseUrl))
                .apiKey(qwenApiKey)
                .restClientBuilder(buildRestClientBuilder())
                .build();
    }

    @Bean("primaryChatModel")
    public OpenAiChatModel primaryChatModel(@Qualifier("primaryOpenAiApi") OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .retryTemplate(buildRetryTemplate(primaryMaxAttempts))
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(primaryModelName)
                        .temperature(0.7)
                        .maxTokens(4096)
                        .build())
                .build();
    }

    @Bean("fallbackChatModel")
    public OpenAiChatModel fallbackChatModel(@Qualifier("fallbackOpenAiApi") OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .retryTemplate(buildRetryTemplate(fallbackMaxAttempts))
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(fallbackModelName)
                        .temperature(0.7)
                        .maxTokens(4096)
                        .build())
                .build();
    }

    private RestClient.Builder buildRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return RestClient.builder().requestFactory(requestFactory);
    }

    private RetryTemplate buildRetryTemplate(int maxAttempts) {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(Math.max(1, maxAttempts)));
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(Math.max(0L, retryBackoffMs));
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    /**
     * OpenAI 兼容协议下，baseUrl 应该是“域名/网关根路径”，不应再包含结尾 /v1。
     * 避免出现 .../v1/v1/chat/completions 这种重复路径。
     */
    private String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String url = raw.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/v1")) {
            url = url.substring(0, url.length() - 3);
        }
        return url;
    }

    @Bean("fallbackChatClient")
    public ChatClient fallbackChatClient(@Qualifier("fallbackChatModel") OpenAiChatModel fallbackChatModel) {
        return ChatClient.builder(fallbackChatModel)
                .defaultSystem("你是一个智能助手，名叫 SmartAgent。你的回答简洁、准确、有帮助。")
                .build();
    }

    /**
     * 基础 ChatClient无记忆
     */
    @Bean("baseChatClient")
    public ChatClient baseChatClient(@Qualifier("primaryChatModel") OpenAiChatModel primaryChatModel) {
        return ChatClient.builder(primaryChatModel)
                .defaultSystem("你是一个智能助手，名叫 SmartAgent。你的回答简洁、准确、有帮助。")
                .build();
    }

    /**
     * 带记忆的 ChatClient
     *
     * 设计要点：
     * - MessageChatMemoryAdvisor 自动在每次请求时加载历史消息，响应后保存新消息
     * - conversationId 在调用时动态传入，实现会话隔离
     * - 系统提示词从外部 .st 文件加载，解耦代码与提示词
     */
    @Bean("memoryChatClient")
    public ChatClient memoryChatClient(@Qualifier("primaryChatModel") OpenAiChatModel primaryChatModel,
                                       ChatMemory chatMemory) {
        return ChatClient.builder(primaryChatModel)
                .defaultSystem(chatSystemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()
                )
                .build();
    }

    /**
     * 带记忆的工具调用 ChatClient
     * 如果 ToolAgent 的 chatWithMemory 方法不生效，使用这个 Bean
     */
    @Bean("toolChatClient")
    public ChatClient toolChatClient(@Qualifier("primaryChatModel") OpenAiChatModel primaryChatModel,
                                     ChatMemory chatMemory,
                                     @Value("classpath:prompts/tool-agent-system.st") Resource toolPrompt) {
        return ChatClient.builder(primaryChatModel)
                .defaultSystem(toolPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * RAG 知识库问答 ChatClient
     *
     * 设计要点：
     * - 使用 QuestionAnswerAdvisor 自动完成 "检索→注入Prompt→回答" 全流程
     * - 结合 Memory 实现知识库的多轮追问
     */
    @Bean("ragChatClient")
    public ChatClient ragChatClient(@Qualifier("primaryChatModel") OpenAiChatModel primaryChatModel,
                                    ChatMemory chatMemory,
                                    VectorStore vectorStore) {
        return ChatClient.builder(primaryChatModel)
                .defaultSystem(ragSystemPrompt)
                .defaultAdvisors(
                        //记忆管理
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        //RAG检索
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .topK(5)
                                                .similarityThreshold(0.5)
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    /**
     * 全能力 Agent ChatClient
     *
     * 整合：记忆 + 工具调用能力
     * RAG 能力通过 RagAgent 单独处理（因为RAG有专用Advisor）
     */
    @Bean("fullAgentClient")
    public ChatClient fullAgentClient(@Qualifier("primaryChatModel") OpenAiChatModel primaryChatModel,
                                      ChatMemory chatMemory) {
        return ChatClient.builder(primaryChatModel)
                .defaultSystem(fullAgentSystemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

}
