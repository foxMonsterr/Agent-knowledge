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
@Document(collection = "chat_messages")
public class ChatMessageDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String messageId;

    @Indexed
    private String sessionId;

    @Indexed
    private String userId;

    private String role;

    private String content;

    @CreatedDate
    private LocalDateTime createdAt;
}
