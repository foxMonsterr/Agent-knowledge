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
 *
 * 存储格式:
 *   Redis key: "chat:memory:{conversationId}"
 *   Redis value: List<Message>(JSON 序列化的消息列表)
 *   TTL: 24 小时(过期自动清理,避免 Redis 内存爆炸)
 */
@Component
@ConditionalOnBean(RedisTemplate.class)
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    /**
     * Redis key 前缀(所有会话消息都加此前缀,便于扫描/清理)
     */
    private static final String KEY_PREFIX = "chat:memory:";

    /**
     * 会话过期时间 24 小时
     * 超过 24 小时未活跃的会话会被 Redis 自动淘汰
     * (用户可重新发起对话创建新会话)
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
     * Spring AI 的 MessageChatMemoryAdvisor 会在每次请求前调用此方法加载历史
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        // 从 Redis 取出原始 value(已被反序列化为 Object)
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
        if (value == null) {
            return new ArrayList<>();
        }

        try {
            // 二次序列化 + 反序列化:确保类型完全匹配 List<Message>
            // 避免 Spring AI 的 Message 子类型(UserMessage/AssistantMessage等)丢失
            JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Message.class);
            return objectMapper.readValue(objectMapper.writeValueAsString(value), type);
        } catch (Exception e) {
            // 反序列化失败:返回空列表(不抛异常,避免拖垮整个对话)
            return new ArrayList<>();
        }
    }

    /**
     * 保存某个会话的全部消息
     * Spring AI 的 MessageChatMemoryAdvisor 会在每次响应后调用此方法保存新消息
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            // 整个 List 覆盖式写入(不是追加),因为 messages 已经是完整历史
            redisTemplate.opsForValue().set(KEY_PREFIX + conversationId, messages, TTL);
        } catch (Exception ignored) {
            // 这里可以按需加日志
        }
    }

    /**
     * 删除指定会话(用户主动清空对话历史时调用)
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
     *
     * 注意: KEYS 命令在生产环境慎用,数据量大时会阻塞 Redis
     *       此处因为会话数量有限(用户级),用 KEYS 还可以接受
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
                // 去掉前缀,只返回纯 sessionId
                conversationIds.add(key.substring(KEY_PREFIX.length()));
            }
        }
        return conversationIds;
    }
}