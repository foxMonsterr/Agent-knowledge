package com.chat.myAgent.conversation.service;

import com.chat.myAgent.conversation.core.ConversationContext;
import com.chat.myAgent.conversation.dto.ConversationMessageVO;
import com.chat.myAgent.conversation.dto.ConversationSummaryVO;
import com.chat.myAgent.model.entity.ChatHistoryEntity;
import com.chat.myAgent.model.entity.ChatSessionEntity;
import com.chat.myAgent.model.vo.SessionVO;
import com.chat.myAgent.repository.ChatHistoryRepository;
import com.chat.myAgent.repository.ChatSessionRepository;
import com.chat.myAgent.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationHistoryService {

    private final AuditService auditService;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatSessionRepository chatSessionRepository;

    public void recordUserMessage(ConversationContext context) {
        auditService.saveChatHistory(context.getConversationId(), context.getUsername(), "user",
                context.getMessage(), context.agentTypeValue(), null, null, null, 0L);
        touchSession(context, abbreviate(context.getMessage(), 48));
    }

    public void recordAssistantMessage(ConversationContext context, String reply, String model, Long latencyMs) {
        auditService.saveChatHistory(context.getConversationId(), context.getUsername(), "assistant",
                reply, context.agentTypeValue(), model, null, null, latencyMs);
        touchSession(context, abbreviate(reply, 48));
    }

    public void recordInvocation(ConversationContext context, String reply, String thinking, String model, String status, Long latencyMs) {
        auditService.saveAgentInvocation(context.getConversationId(), context.agentTypeValue(), model,
                context.getMessage(), reply, thinking, status, latencyMs);
    }

    public List<ConversationMessageVO> listMessages(String username, String conversationId) {
        return chatHistoryRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .filter(item -> isOwner(username, item))
                .map(this::toMessage)
                .toList();
    }

    public List<ConversationSummaryVO> listConversations(String username, String agentType, String keyword) {
        Set<String> conversationIds = new LinkedHashSet<>();
        chatSessionRepository.findByUsernameOrderByLastMessageAtDesc(username).stream()
                .map(ChatSessionEntity::getSessionId)
                .filter(id -> id != null && !id.isBlank())
                .forEach(conversationIds::add);
        conversationIds.addAll(chatHistoryRepository.findDistinctConversationIdsByUsername(username));

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return conversationIds.stream()
                .map(conversationId -> toSummary(username, conversationId, formatter))
                .filter(item -> agentType == null || agentType.isBlank() || item.getAgentTypes().contains(agentType))
                .filter(item -> keyword == null || keyword.isBlank()
                        || containsIgnoreCase(item.getTitle(), keyword)
                        || containsIgnoreCase(item.getSummary(), keyword))
                .sorted(Comparator.comparing(ConversationSummaryVO::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public SessionVO createConversation(String username, String title) {
        String conversationId = UUID.randomUUID().toString().replace("-", "");
        String resolvedTitle = title == null || title.isBlank() ? "新会话" : title.trim();
        ChatSessionEntity sessionEntity = new ChatSessionEntity();
        sessionEntity.setSessionId(conversationId);
        sessionEntity.setUsername(username);
        sessionEntity.setTitle(resolvedTitle);
        sessionEntity.setSummary("新会话");
        sessionEntity.setStatus("active");
        sessionEntity.setLastMessageAt(LocalDateTime.now());
        chatSessionRepository.save(sessionEntity);
        return SessionVO.builder()
                .sessionId(conversationId)
                .title(resolvedTitle)
                .summary("新会话")
                .status("active")
                .lastMessageAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    @Transactional
    public SessionVO renameConversation(String username, String conversationId, String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        assertOwner(username, conversationId);
        ChatSessionEntity session = chatSessionRepository.findById(conversationId).orElseGet(() -> {
            ChatSessionEntity entity = new ChatSessionEntity();
            entity.setSessionId(conversationId);
            entity.setUsername(username);
            entity.setStatus("active");
            entity.setLastMessageAt(LocalDateTime.now());
            return entity;
        });
        session.setTitle(title.trim());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
        return SessionVO.builder()
                .sessionId(conversationId)
                .title(session.getTitle())
                .summary(session.getSummary())
                .status(session.getStatus())
                .lastMessageAt(session.getLastMessageAt() == null ? null : session.getLastMessageAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    @Transactional
    public void deleteConversation(String username, String conversationId) {
        assertOwner(username, conversationId);
        chatHistoryRepository.deleteByConversationId(conversationId);
        chatSessionRepository.deleteById(conversationId);
    }

    public void assertOwner(String username, String conversationId) {
        if (username == null || username.isBlank() || conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("会话不存在或无权访问");
        }
        boolean ownedBySession = chatSessionRepository.findById(conversationId)
                .map(session -> username.equals(session.getUsername()))
                .orElse(false);
        boolean ownedByHistory = chatHistoryRepository.existsByConversationIdAndUsername(conversationId, username);
        if (!ownedBySession && !ownedByHistory) {
            throw new IllegalArgumentException("会话不存在或无权访问");
        }
    }

    private void touchSession(ConversationContext context, String summary) {
        ChatSessionEntity session = chatSessionRepository.findById(context.getConversationId()).orElseGet(() -> {
            ChatSessionEntity entity = new ChatSessionEntity();
            entity.setSessionId(context.getConversationId());
            entity.setUsername(context.getUsername());
            entity.setTitle(abbreviate(context.getMessage(), 18));
            entity.setStatus("active");
            return entity;
        });
        if (session.getUsername() == null || session.getUsername().isBlank()) {
            session.setUsername(context.getUsername());
        }
        session.setSummary(summary);
        session.setLastMessageAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
    }

    private ConversationMessageVO toMessage(ChatHistoryEntity item) {
        return ConversationMessageVO.builder()
                .role(item.getMessageRole())
                .content(item.getContent())
                .agentType(item.getAgentType())
                .model(item.getModel())
                .latencyMs(item.getLatencyMs())
                .createdAt(item.getCreatedAt() == null ? null : item.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private ConversationSummaryVO toSummary(String username, String conversationId, DateTimeFormatter formatter) {
        List<ChatHistoryEntity> thread = chatHistoryRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .filter(item -> isOwner(username, item))
                .toList();
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
                : last == null ? null : last.getCreatedAt();
        return ConversationSummaryVO.builder()
                .conversationId(conversationId)
                .title(title)
                .summary(summary)
                .status(meta != null && meta.getStatus() != null ? meta.getStatus() : "active")
                .agentTypes(thread.stream().map(ChatHistoryEntity::getAgentType).filter(v -> v != null && !v.isBlank()).distinct().toList())
                .lastMessageAt(lastAt == null ? null : lastAt.format(formatter))
                .createdAt(meta == null || meta.getCreatedAt() == null ? null : meta.getCreatedAt().format(formatter))
                .updatedAt(meta == null || meta.getUpdatedAt() == null ? null : meta.getUpdatedAt().format(formatter))
                .build();
    }

    private boolean isOwner(String username, ChatHistoryEntity item) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return username.equals(item.getUsername());
    }

    private String buildSessionTitle(List<ChatHistoryEntity> thread) {
        if (thread == null || thread.isEmpty()) {
            return "未命名会话";
        }
        return thread.stream()
                .filter(item -> "user".equalsIgnoreCase(item.getMessageRole()) && item.getContent() != null && !item.getContent().isBlank())
                .map(item -> abbreviate(item.getContent(), 18))
                .findFirst()
                .orElse("未命名会话");
    }

    private String buildSessionSummary(List<ChatHistoryEntity> thread) {
        if (thread == null || thread.isEmpty()) {
            return "暂无消息";
        }
        ChatHistoryEntity last = thread.get(thread.size() - 1);
        String role = last.getMessageRole() == null ? "" : last.getMessageRole();
        return role + ": " + abbreviate(last.getContent(), 48);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && keyword != null && text.toLowerCase().contains(keyword.toLowerCase());
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > maxLen ? clean.substring(0, maxLen) + "..." : clean;
    }
}
