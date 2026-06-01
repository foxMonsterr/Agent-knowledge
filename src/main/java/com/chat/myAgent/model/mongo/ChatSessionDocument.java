package com.chat.myAgent.model.mongo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "chat_sessions")
public class ChatSessionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    @Indexed
    private String userId;

    private String title;

    private String summary;

    private String status;

    @Indexed
    private LocalDateTime lastMessageAt;

    @CreatedDate
    private LocalDateTime createdAt;
}
