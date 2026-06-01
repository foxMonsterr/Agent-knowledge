package com.chat.myAgent.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MemoryConfig {

    @Bean
    @Primary
    public ChatMemory chatMemory(ObjectProvider<RedisChatMemoryRepository> redisRepoProvider) {
        RedisChatMemoryRepository redisRepo = redisRepoProvider.getIfAvailable();
        if (redisRepo != null) {
            return MessageWindowChatMemory.builder()
                    .chatMemoryRepository(redisRepo)
                    .maxMessages(50)
                    .build();
        }
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(50)
                .build();
    }

}
