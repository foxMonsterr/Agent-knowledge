package com.chat.myAgent.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * API 限流拦截器
 *
 * 设计说明：
 * - 基于滑动窗口的限流算法
 * - 优先使用 Redis（分布式限流），Redis 不可用时使用内存（单机限流）
 * - 按用户（Token中的username）或 IP 限流
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${smart-agent.rate-limit.max-requests-per-minute:30}")
    private int maxRequestsPerMinute;

    private final StringRedisTemplate redisTemplate;

    // 内存回退方案
    private final Map<String, Deque<Long>> memoryLimiter = new ConcurrentHashMap<>();

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // 只限制 API 接口
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }

        // 提取客户端标识
        String clientKey = extractClientKey(request);

        boolean allowed;
        try {
            allowed = checkWithRedis(clientKey);
        } catch (Exception e) {
            // Redis 不可用，回退到内存限流
            log.debug("Redis 不可用，使用内存限流");
            allowed = checkWithMemory(clientKey);
        }

        if (!allowed) {
            log.warn("API 限流触发: key={}, limit={}/min", clientKey, maxRequestsPerMinute);
            writeRateLimitResponse(response);
            return false;
        }

        return true;
    }

    /**
     * Redis 限流（分布式）
     */
    private boolean checkWithRedis(String clientKey) {
        // 构造 Redis Key，如 "rate_limit:192.168.1.100"
        String key = "rate_limit:" + clientKey;
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000; // 1分钟窗口

        // 1. 清理过期请求
        // removeRangeByScore：删除 score 在 [0, windowStart] 范围内的所有成员
        // 即删除 1 分钟前的所有请求记录
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // 2. 统计当前窗口内的请求数
        // zCard：获取 Sorted Set 的成员数量
        Long count = redisTemplate.opsForZSet().zCard(key);

        // 3. 判断是否超过限流阈值
        if (count != null && count >= maxRequestsPerMinute) {
            return false;
        }

        // 4. 记录本次请求
        // add(key, member, score)：添加成员
        // member = now（请求标识）
        // score = now（用于排序和清理）
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);

        // 5. 设置过期时间
        // 2 分钟后自动删除，防止冷数据堆积
        redisTemplate.expire(key, 2, TimeUnit.MINUTES); // 2分钟后自动过期
        return true;
    }

    /**
     * 内存限流（单机回退方案）
     */
    private boolean checkWithMemory(String clientKey) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000;

        Deque<Long> timestamps = memoryLimiter.computeIfAbsent(clientKey, k -> new ConcurrentLinkedDeque<>());

        // 清理窗口外的记录
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequestsPerMinute) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanupMemory() {
        long now = System.currentTimeMillis();
        long threshold = now - 120_000; // 2分钟无请求则清理
        Iterator<Map.Entry<String, Deque<Long>>> it = memoryLimiter.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<Long>> entry = it.next();
            Deque<Long> deque = entry.getValue();
            while (!deque.isEmpty() && deque.peekFirst() < threshold) {
                deque.pollFirst();
            }
            if (deque.isEmpty()) {
                it.remove();
            }
        }
    }

    /**
     * 提取客户端标识（优先用户名，其次IP）
     */
    private String extractClientKey(HttpServletRequest request) {
        // 尝试从 JWT 中获取用户名
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 简单提取（不做完整JWT解析，只用于限流标识）
            return "user:" + authHeader.substring(7, Math.min(25, authHeader.length()));
        }

        // 回退到 IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip;
    }

    /**
     * 写入限流响应
     */
    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试（限制" + maxRequestsPerMinute + "次/分钟）\",\"data\":null}");
    }
}
