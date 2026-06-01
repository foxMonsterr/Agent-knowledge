package com.chat.myAgent.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * 功能说明：
 * 1. 配置 RedisTemplate Bean，用于 Redis 的读写操作
 * 2. 自定义序列化方式，支持 Java 对象与 JSON 的自动转换
 * 3. 为 RedisChatMemoryRepository 提供 Redis 操作能力
 *
 * 序列化策略：
 * - Key：String 序列化（人类可读）
 * - Value：JSON 序列化（支持复杂对象，跨语言兼容）
 *
 * 使用场景：
 * - ChatMemory 对话记忆持久化
 * - 限流计数器（RateLimitInterceptor）
 * - 后续扩展缓存能力
 */
@Configuration
public class RedisConfig {

    /**
     * 创建 RedisTemplate Bean
     *
     * RedisTemplate 是 Spring Data Redis 提供的核心操作类，
     * 封装了 Redis 的各种操作（get/set/delete/hash/list 等）
     *
     * @param connectionFactory Redis 连接工厂（Spring Boot 自动配置）
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        // 设置 Redis 连接工厂（由 Spring Boot 根据 application.yml 自动配置）
        // 连接工厂负责创建 Redis 连接，包含连接池管理
        template.setConnectionFactory(connectionFactory);

        // ==================== 序列化器配置 ====================
        
        // String 序列化器：用于 Key 的序列化
        // 将 Key 序列化为普通字符串，如 "chat:memory:session-001"
        // 优点：可读，便于调试和监控
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 创建 ObjectMapper 用于 JSON 序列化
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 设置所有属性可见（包括 private 属性）
        // 这样即使对象的属性是 private，也能被序列化
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // 启用多态类型处理
        // 在 JSON 中存储对象的类型信息，反序列化时能正确还原为原始类型
        // 例如：["java.util.ArrayList", [...]] 会还原为 ArrayList 而非 LinkedList
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType("org.springframework.ai.chat.messages.")
                        .allowIfBaseType("java.util.")
                        .allowIfBaseType("java.lang.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // JSON 序列化器：用于 Value 的序列化
        // 将 Java 对象序列化为 JSON 字符串存储到 Redis
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // ==================== 设置序列化器 ====================
        
        // Key 序列化器：Redis 的 Key 使用 String 序列化
        template.setKeySerializer(stringRedisSerializer);
        
        // Hash Key 序列化器：Hash 结构的 field 使用 String 序列化
        template.setHashKeySerializer(stringRedisSerializer);
        
        // Value 序列化器：Redis 的 Value 使用 JSON 序列化
        // 支持存储复杂对象（如 List<Message>）
        template.setValueSerializer(serializer);
        
        // Hash Value 序列化器：Hash 结构的 value 使用 JSON 序列化
        template.setHashValueSerializer(serializer);

        // 初始化配置（必须调用，否则配置不生效）
        template.afterPropertiesSet();
        
        return template;
    }
}