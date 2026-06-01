package com.chat.myAgent.repository.mongo;

import com.chat.myAgent.model.mongo.ChatMessageDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessageDocument, String> {

    List<ChatMessageDocument> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    Page<ChatMessageDocument> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);

    List<ChatMessageDocument> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByMessageId(String messageId);
}
