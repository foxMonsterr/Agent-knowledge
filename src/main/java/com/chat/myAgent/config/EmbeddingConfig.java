package com.chat.myAgent.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 模型配置
 *
 * 使用阿里云 DashScope（Qwen）的 Embedding 服务
 * 通过 OpenAI 兼容模式调用
 *
 * 支持的模型：
 * - text-embedding-v3：通用场景首选，支持 1024/768/512 等维度
 * - text-embedding-v4：最新旗舰，支持 2048/1536 高维向量
 */
@Configuration
public class EmbeddingConfig {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.base-url:}")
    private String dashScopeBaseUrl;

    @Value("${dashscope.embedding-model:text-embedding-v4}")
    private String embeddingModel;

    /**
     * 创建 Embedding 模型 Bean
     *
     * 功能说明：
     * - 将文本转换为向量（Embedding），用于 RAG 知识库的相似度检索
     * - 使用 OpenAI 兼容模式调用阿里云 DashScope API
     *
     * 关键参数说明：
     * - embeddingsPath("/embeddings")：指定 API 路径，避免 Spring AI 自动添加 /v1 前缀
     * - MetadataMode.EMBED：只将元数据用于 Embedding，不包含在最终输出中
     *
     * @return OpenAiEmbeddingModel 实例，会被 Spring AI 自动注入到 VectorStore
     */
    @Bean
    public OpenAiEmbeddingModel embeddingModel() {
        // 创建 OpenAiApi 客户端
        // baseUrl: DashScope 的 OpenAI 兼容端点
        // apiKey: 从环境变量读取的 API Key
        // embeddingsPath: 自定义路径，避免重复的 /v1 前缀
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(dashScopeBaseUrl))
                .apiKey(dashScopeApiKey)
                .embeddingsPath("/embeddings")
                .build();

        // 创建 Embedding 选项
        // model: 指定使用的 Embedding 模型名称
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModel)
                .build();

        // 创建并返回 OpenAiEmbeddingModel 实例
        // 参数1: OpenAiApi 客户端
        // 参数2: MetadataMode.EMBED - 元数据处理模式
        // 参数3: Embedding 选项（模型名称等）
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

    private String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String url = raw.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
