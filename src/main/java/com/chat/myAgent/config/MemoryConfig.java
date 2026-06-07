package com.chat.myAgent.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ChatMemory 配置
 *
 * 职责:为 ChatClient 提供"多轮对话记忆"能力
 *
 * 设计要点:
 * 1. 优先使用 Redis 持久化(支持服务重启后恢复历史)
 * 2. 无 Redis 时降级到内存(InMemoryChatMemoryRepository)
 * 3. 滑动窗口 maxMessages=50:只保留最近 50 条消息,避免上下文无限膨胀
 *    (超过 50 条会被 MessageWindowChatMemory 自动丢弃最早的)
 *
 * 通过 @Primary 标记为本应用的默认 ChatMemory Bean
 */
@Configuration
public class MemoryConfig {

    /**
     * 构造 ChatMemory Bean
     * @param redisRepoProvider RedisChatMemoryRepository 由 Spring 容器按需注入
     *                          - 有 RedisTemplate 时该 Bean 存在
     *                          - 无 RedisTemplate 时该 Bean 不存在
     *                          ObjectProvider.getIfAvailable() 容错处理
     */
    @Bean
    @Primary
    public ChatMemory chatMemory(ObjectProvider<RedisChatMemoryRepository> redisRepoProvider) {
        RedisChatMemoryRepository redisRepo = redisRepoProvider.getIfAvailable();
        if (redisRepo != null) {
            // 生产环境: Redis 持久化,服务重启后历史不丢
            return MessageWindowChatMemory.builder()
                    .chatMemoryRepository(redisRepo)
                    .maxMessages(50)
                    .build();
        }
        // 开发环境/无 Redis: 内存版,重启后清空
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(50)
                .build();
    }

}
