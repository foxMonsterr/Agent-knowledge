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
 *
 * 设计要点:
 * 1. 三级降级链路: baseChatClient → memoryChatClient → fallbackChatClient
 * 2. 实例隔离: 主备两套独立的 OpenAiApi,避免连接池污染
 * 3. 快速失败: maxAttempts=1,主模型不重试,直接降级
 * 4. 多种 ChatClient Bean: 无记忆/带记忆/工具调用/RAG/全能力,按场景选用
 */
@Configuration
public class ChatClientConfig {

    // ========== 模型名称配置 ==========
    @Value("${smart-agent.models.default-model:deepseek-chat}")
    private String primaryModelName;

    @Value("${smart-agent.models.fallback-model:qwen3.6-plus}")
    private String fallbackModelName;

    // ========== 主模型连接(DeepSeek) ==========
    @Value("${deepseek.api-base-url:${spring.ai.openai.base-url:https://api.deepseek.com}}")
    private String deepseekBaseUrl;

    @Value("${deepseek.api-key:${DEEPSEEK_API_KEY:}}")
    private String deepseekApiKey;

    // ========== 备用模型连接(Qwen) ==========
    @Value("${qwen.api-base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String qwenBaseUrl;

    @Value("${qwen.api-key:${QWEN_API_KEY:}}")
    private String qwenApiKey;

    // ========== 网络超时与重试 ==========
    @Value("${smart-agent.network.connect-timeout:8s}")
    private Duration connectTimeout;

    @Value("${smart-agent.network.read-timeout:25s}")
    private Duration readTimeout;

    // 主模型最大尝试次数(默认 1,即不重试,直接降级)
    @Value("${smart-agent.retry.primary.max-attempts:1}")
    private int primaryMaxAttempts;

    // 备用模型最大尝试次数
    @Value("${smart-agent.retry.fallback.max-attempts:1}")
    private int fallbackMaxAttempts;

    @Value("${smart-agent.retry.backoff-ms:200}")
    private long retryBackoffMs;

    // ========== 系统提示词(从外部 .st 文件加载) ==========
    // 解耦代码与提示词,提示词工程师可直接修改 .st 文件而无需改 Java
    @Value("classpath:prompts/chat-system.st")
    private Resource chatSystemPrompt;

    @Value("classpath:prompts/rag-system.st")
    private Resource ragSystemPrompt;

    @Value("classpath:prompts/full-agent-system.st")
    private Resource fullAgentSystemPrompt;

    /**
     * 主模型 OpenAI 兼容 API 客户端
     * 独立 Bean:与备用模型不共享连接池,主挂掉时切换不被污染
     */
    @Bean("primaryOpenAiApi")
    public OpenAiApi primaryOpenAiApi() {
        return OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(deepseekBaseUrl))
                .apiKey(deepseekApiKey)
                .restClientBuilder(buildRestClientBuilder())
                .build();
    }

    /**
     * 备用模型 OpenAI 兼容 API 客户端
     * 与主模型完全隔离,主模型故障时不影响备用
     */
    @Bean("fallbackOpenAiApi")
    public OpenAiApi fallbackOpenAiApi() {
        return OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(qwenBaseUrl))
                .apiKey(qwenApiKey)
                .restClientBuilder(buildRestClientBuilder())
                .build();
    }

    /**
     * 主模型 ChatModel
     * - retryTemplate(maxAttempts=1): 不重试,快速降级
     * - temperature=0.7: 平衡创造性与稳定性
     * - maxTokens=4096: 适配长文本生成(ReAct 总结可能较长)
     */
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

    /**
     * 备用模型 ChatModel
     * 配置同主模型,独立实例
     */
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

    /**
     * 构造带超时配置的 RestClient
     * connectTimeout: TCP 连接超时(默认 8s)
     * readTimeout: 等待响应超时(默认 25s,ReAct 长链路需要更长)
     */
    private RestClient.Builder buildRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * 构造重试模板
     * maxAttempts=1: 不重试,LLM API 故障通常持续较久,重试只会增加等待
     * backoff=200ms: 预留扩展空间(后续要做指数退避可改这里)
     */
    private RetryTemplate buildRetryTemplate(int maxAttempts) {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(Math.max(1, maxAttempts)));
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(Math.max(0L, retryBackoffMs));
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    /**
     * OpenAI 兼容协议下,baseUrl 应该是"域名/网关根路径",不应再包含结尾 /v1。
     * 避免出现 .../v1/v1/chat/completions 这种重复路径。
     */
    private String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String url = raw.trim();
        // 去掉末尾 /
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        // 去掉末尾 /v1(Spring AI 会自动拼 /v1/chat/completions)
        if (url.endsWith("/v1")) {
            url = url.substring(0, url.length() - 3);
        }
        return url;
    }

    /**
     * 兜底 ChatClient(三级降级的第二级)
     * 用最简系统提示词,确保备用模型也能给出"还凑合"的答案
     */
    @Bean("fallbackChatClient")
    public ChatClient fallbackChatClient(@Qualifier("fallbackChatModel") OpenAiChatModel fallbackChatModel) {
        return ChatClient.builder(fallbackChatModel)
                .defaultSystem("你是一个智能助手，名叫 SmartAgent。你的回答简洁、准确、有帮助。")
                .build();
    }

    /**
     * 基础 ChatClient(无会话记忆)
     * 适用场景:单次问答、批处理任务、不需要上下文的场景
     */
    @Bean("baseChatClient")
    public ChatClient baseChatClient(@Qualifier("primaryChatModel") OpenAiChatModel primaryChatModel) {
        return ChatClient.builder(primaryChatModel)
                .defaultSystem("你是一个智能助手，名叫 SmartAgent。你的回答简洁、准确、有帮助。")
                .build();
    }

    /**
     * 带会话记忆的 ChatClient
     *
     * 设计要点：
     * - MessageChatMemoryAdvisor 自动在每次请求时加载历史消息,响应后保存新消息
     * - conversationId 在调用时动态传入,实现会话隔离
     * - 系统提示词从外部 .st 文件加载,解耦代码与提示词
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
     * 用于 ReAct Agent(结合 @Tool 注解的函数)
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
     *
     * 这是"传统 RAG"模式:向量检索是 Spring AI Advisor 自动完成的
     * (与 ReAct 模式不同,ReAct 把检索作为 Profile.selectTools() 中的一个工具)
     */
    @Bean("ragChatClient")
    public ChatClient ragChatClient(@Qualifier("primaryChatModel") OpenAiChatModel primaryChatModel,
                                    ChatMemory chatMemory,
                                    VectorStore vectorStore) {
        return ChatClient.builder(primaryChatModel)
                .defaultSystem(ragSystemPrompt)
                .defaultAdvisors(
                        // 记忆管理
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // RAG 检索:自动把向量库检索结果注入到 prompt
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .topK(5)              // 检索 top 5
                                                .similarityThreshold(0.5)  // 相似度阈值
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    /**
     * 全能力 Agent ChatClient
     *
     * 整合:记忆 + 工具调用能力
     * RAG 能力通过 RagAgent 单独处理(因为RAG有专用Advisor)
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
