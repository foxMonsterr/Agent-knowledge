package com.chat.myAgent.monitor;

import com.chat.myAgent.model.dto.ConversationRecord;
import com.chat.myAgent.model.entity.ChatHistoryEntity;
import com.chat.myAgent.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token 使用追踪器
 *
 * 记录每次AI调用的Token消耗、耗时、成本
 * 同时将记录持久化到数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUsageTracker {

    private final ChatHistoryRepository chatHistoryRepository;

    // 运行时统计（内存中，快速读取）
    private final AtomicLong sessionPromptTokens = new AtomicLong(0);
    private final AtomicLong sessionCompletionTokens = new AtomicLong(0);
    private final AtomicLong sessionCalls = new AtomicLong(0);

    /**
     * 记录一次对话（持久化到数据库）
     *
     * @param record 对话记录对象，包含所有必要信息
     */
    public void trackConversation(ConversationRecord record) {
        String username = record.getUsername() != null ? record.getUsername() : "anonymous";

        // 保存用户消息
        ChatHistoryEntity userRecord = new ChatHistoryEntity();
        BeanUtils.copyProperties(record, userRecord, "username", "userMessage", "aiReply", "promptTokens", "completionTokens", "latencyMs");
        userRecord.setUsername(username);
        userRecord.setMessageRole("user");
        userRecord.setContent(record.getUserMessage());
        chatHistoryRepository.save(userRecord);

        // 保存AI回复
        ChatHistoryEntity aiRecord = new ChatHistoryEntity();
        BeanUtils.copyProperties(record, aiRecord, "username", "userMessage", "aiReply");
        aiRecord.setUsername(username);
        aiRecord.setMessageRole("assistant");
        aiRecord.setContent(record.getAiReply());
        chatHistoryRepository.save(aiRecord);

        // 更新运行时统计
        if (record.getPromptTokens() != null) sessionPromptTokens.addAndGet(record.getPromptTokens());
        if (record.getCompletionTokens() != null) sessionCompletionTokens.addAndGet(record.getCompletionTokens());
        sessionCalls.incrementAndGet();

        log.info("[AI调用] conv={}, user={}, agent={}, tokens={}+{}, latency={}ms",
                record.getConversationId(), record.getUsername(), record.getAgentType(),
                record.getPromptTokens(), record.getCompletionTokens(), record.getLatencyMs());
    }

    /**
     * 获取运行时统计
     */
    public Map<String, Object> getSessionStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("sessionCalls", sessionCalls.get());
        stats.put("sessionPromptTokens", sessionPromptTokens.get());
        stats.put("sessionCompletionTokens", sessionCompletionTokens.get());
        stats.put("sessionTotalTokens", sessionPromptTokens.get() + sessionCompletionTokens.get());
        return stats;
    }

    /**
     * 获取全量统计（从数据库）
     */
    public Map<String, Object> getAllTimeStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalRecords = chatHistoryRepository.count();
        Long totalPrompt = chatHistoryRepository.sumPromptTokens();
        Long totalCompletion = chatHistoryRepository.sumCompletionTokens();
        Long totalTokens = chatHistoryRepository.sumTotalTokens();

        // 今日统计
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long todayCalls = chatHistoryRepository.countByCreatedAtBetween(todayStart, todayEnd);

        stats.put("totalRecords", totalRecords);
        stats.put("totalPromptTokens", totalPrompt != null ? totalPrompt : 0);
        stats.put("totalCompletionTokens", totalCompletion != null ? totalCompletion : 0);
        stats.put("totalTokens", totalTokens != null ? totalTokens : 0);
        stats.put("todayCalls", todayCalls);
        stats.put("estimatedCost", estimateCost(totalPrompt, totalCompletion));

        return stats;
    }

    /**
     * 成本估算
     */
    private String estimateCost(Long promptTokens, Long completionTokens) {
        if (promptTokens == null) promptTokens = 0L;
        if (completionTokens == null) completionTokens = 0L;

        // DeepSeek V4 Flash 参考价格（根据实际调整）
        double inputCost = promptTokens / 1_000_000.0 * 1.0;   // ¥1/百万Token
        double outputCost = completionTokens / 1_000_000.0 * 2.0; // ¥2/百万Token
        return String.format("¥%.4f", inputCost + outputCost);
    }
}
