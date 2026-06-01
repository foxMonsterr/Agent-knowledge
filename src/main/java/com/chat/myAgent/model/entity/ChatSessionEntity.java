package com.chat.myAgent.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话元数据实体
 *
 * 用于持久化会话标题、摘要等可编辑信息，避免标题仅依赖首条消息自动推导。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_chat_session")
public class ChatSessionEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String sessionId;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 30)
    private String status;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = "active";
        }
    }
}
