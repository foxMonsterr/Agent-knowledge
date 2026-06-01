package com.chat.myAgent.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Redis 持久化 ChatMemoryRepository
 *
 * 作用：
 * 1. 将多轮对话上下文保存到 Redis
 * 2. 服务重启后仍可恢复历史消息
 * 3. 支持查询所有会话 ID，兼容 Spring AI 新版接口
 */
@Component
@ConditionalOnBean(RedisTemplate.class)
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    /**
     * Redis key 前缀
     */
    private static final String KEY_PREFIX = "chat:memory:";

    /**
     * 会话过期时间
     */
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据会话 ID 查询消息列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
        if (value == null) {
            return new ArrayList<>();
        }

        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Message.class);
            return objectMapper.readValue(objectMapper.writeValueAsString(value), type);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 保存某个会话的全部消息
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + conversationId, messages, TTL);
        } catch (Exception ignored) {
            // 这里可以按需加日志
        }
    }

    /**
     * 删除指定会话
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    /**
     * 查询当前 Redis 中保存的所有会话 ID
     *
     * Spring AI 新版 ChatMemoryRepository 接口要求实现该方法。
     * 这里通过扫描 Redis 中的 key 来获取会话列表。
     */
    @Override
    public List<String> findConversationIds() {
        List<String> conversationIds = new ArrayList<>();

        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return conversationIds;
        }

        for (String key : keys) {
            if (key != null && key.startsWith(KEY_PREFIX)) {
                conversationIds.add(key.substring(KEY_PREFIX.length()));
            }
        }
        return conversationIds;
    }
}