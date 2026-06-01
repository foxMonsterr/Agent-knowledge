package com.chat.myAgent.controller;

import com.chat.myAgent.agent.ChatAgent;
import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.dto.CreateSessionRequest;
import com.chat.myAgent.model.dto.UpdateSessionTitleRequest;
import com.chat.myAgent.model.entity.ChatHistoryEntity;
import com.chat.myAgent.model.entity.ChatSessionEntity;
import com.chat.myAgent.model.vo.SessionVO;
import com.chat.myAgent.repository.ChatHistoryRepository;
import com.chat.myAgent.repository.ChatSessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "会话管理", description = "查看会话历史、清除会话记忆、管理会话标题")
@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class SessionController {

    private final ChatAgent chatAgent;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatSessionRepository chatSessionRepository;

    @Operation(summary = "创建会话", description = "创建一个新的 conversationId，会话历史在首次发消息后落库")
    @PostMapping("/create")
    public R<SessionVO> createSession(@RequestBody(required = false) CreateSessionRequest request) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String title = request != null && request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle().trim() : "新会话";

        ChatSessionEntity sessionEntity = new ChatSessionEntity();
        sessionEntity.setSessionId(sessionId);
        sessionEntity.setTitle(title);
        sessionEntity.setSummary("新会话");
        sessionEntity.setStatus("active");
        sessionEntity.setLastMessageAt(LocalDateTime.now());
        chatSessionRepository.save(sessionEntity);

        SessionVO session = SessionVO.builder()
                .sessionId(sessionId)
                .title(title)
                .summary("新会话")
                .status("active")
                .lastMessageAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        return R.ok("会话创建成功", session);
    }

    @Operation(summary = "获取会话列表", description = "获取当前系统中所有 conversationId 会话的摘要列表")
    @GetMapping("/list")
    public R<List<SessionVO>> listSessions() {
        List<String> conversationIds = chatHistoryRepository.findDistinctConversationIds();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        List<SessionVO> sessions = conversationIds.stream()
                .map(conversationId -> {
                    List<ChatHistoryEntity> thread = chatHistoryRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
                    ChatHistoryEntity last = thread.isEmpty() ? null : thread.get(thread.size() - 1);
                    ChatSessionEntity meta = chatSessionRepository.findById(conversationId).orElse(null);
                    String title = meta != null && meta.getTitle() != null && !meta.getTitle().isBlank()
                            ? meta.getTitle()
                            : buildSessionTitle(thread);
                    String summary = meta != null && meta.getSummary() != null && !meta.getSummary().isBlank()
                            ? meta.getSummary()
                            : buildSessionSummary(thread);
                    LocalDateTime lastAt = meta != null && meta.getLastMessageAt() != null
                            ? meta.getLastMessageAt()
                            : last != null ? last.getCreatedAt() : null;
                    return SessionVO.builder()
                            .sessionId(conversationId)
                            .title(title)
                            .summary(summary)
                            .lastMessageAt(lastAt != null ? lastAt.format(formatter) : null)
                            .status(meta != null && meta.getStatus() != null ? meta.getStatus() : "active")
                            .build();
                })
                .sorted(Comparator.comparing(SessionVO::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return R.ok(sessions);
    }

    @Operation(summary = "获取指定会话的历史消息")
    @GetMapping("/{conversationId}/history")
    public R<List<Map<String, String>>> getHistory(@Parameter(description = "会话ID", required = true) @PathVariable String conversationId) {
        List<ChatHistoryEntity> histories = chatHistoryRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Map<String, String>> history = histories.stream()
                .map(item -> {
                    Map<String, String> row = new HashMap<>();
                    row.put("role", item.getMessageRole());
                    row.put("content", item.getContent());
                    row.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                    return row;
                })
                .collect(Collectors.toList());

        return R.ok(history);
    }

    @Operation(summary = "重命名会话", description = "修改会话展示标题")
    @PatchMapping("/{conversationId}/title")
    @Transactional
    public R<SessionVO> updateTitle(@PathVariable String conversationId, @RequestBody UpdateSessionTitleRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            return R.paramError("标题不能为空");
        }
        String title = request.getTitle().trim();
        ChatSessionEntity session = chatSessionRepository.findById(conversationId).orElseGet(() -> {
            ChatSessionEntity entity = new ChatSessionEntity();
            entity.setSessionId(conversationId);
            entity.setStatus("active");
            entity.setLastMessageAt(LocalDateTime.now());
            return entity;
        });
        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        return R.ok(SessionVO.builder()
                .sessionId(conversationId)
                .title(title)
                .summary(session.getSummary())
                .status(session.getStatus())
                .lastMessageAt(session.getLastMessageAt() != null ? session.getLastMessageAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build());
    }

    @Operation(summary = "导出会话", description = "导出指定会话的完整历史内容")
    @GetMapping("/{conversationId}/export")
    public R<Map<String, Object>> exportSession(@PathVariable String conversationId) {
        ChatSessionEntity meta = chatSessionRepository.findById(conversationId).orElse(null);
        List<ChatHistoryEntity> histories = chatHistoryRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("title", meta != null && meta.getTitle() != null ? meta.getTitle() : buildSessionTitle(histories));
        result.put("summary", meta != null && meta.getSummary() != null ? meta.getSummary() : buildSessionSummary(histories));
        result.put("status", meta != null ? meta.getStatus() : "active");
        result.put("createdAt", meta != null && meta.getCreatedAt() != null ? meta.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        result.put("updatedAt", meta != null && meta.getUpdatedAt() != null ? meta.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        result.put("messages", histories.stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("role", item.getMessageRole());
            row.put("content", item.getContent());
            row.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            row.put("agentType", item.getAgentType());
            row.put("model", item.getModel());
            row.put("promptTokens", item.getPromptTokens());
            row.put("completionTokens", item.getCompletionTokens());
            row.put("latencyMs", item.getLatencyMs());
            return row;
        }).collect(Collectors.toList()));

        return R.ok(result);
    }

    @Operation(summary = "清除会话记忆", description = "清除后该conversationId的对话将从头开始")
    @Transactional
    @DeleteMapping("/{conversationId}")
    public R<String> clearSession(@Parameter(description = "会话ID", required = true) @PathVariable String conversationId) {
        chatAgent.clearMemory(conversationId);
        chatHistoryRepository.deleteByConversationId(conversationId);
        chatSessionRepository.deleteById(conversationId);
        return R.ok("会话 [" + conversationId + "] 已清除");
    }

    private String buildSessionTitle(List<ChatHistoryEntity> thread) {
        if (thread == null || thread.isEmpty()) return "未命名会话";
        return thread.stream()
                .filter(item -> "user".equalsIgnoreCase(item.getMessageRole()) && item.getContent() != null && !item.getContent().isBlank())
                .map(item -> abbreviate(item.getContent(), 18))
                .findFirst()
                .orElse("未命名会话");
    }

    private String buildSessionSummary(List<ChatHistoryEntity> thread) {
        if (thread == null || thread.isEmpty()) return "暂无消息";
        return thread.stream()
                .reduce((first, last) -> last)
                .map(last -> {
                    String role = last.getMessageRole() == null ? "" : last.getMessageRole();
                    return role + ": " + abbreviate(last.getContent(), 48);
                })
                .orElse("暂无消息");
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > maxLen ? clean.substring(0, maxLen) + "..." : clean;
    }
}
