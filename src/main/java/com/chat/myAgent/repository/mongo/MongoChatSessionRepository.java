package com.chat.myAgent.repository.mongo;

import com.chat.myAgent.model.mongo.ChatSessionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MongoChatSessionRepository extends MongoRepository<ChatSessionDocument, String> {

    Optional<ChatSessionDocument> findBySessionId(String sessionId);

    List<ChatSessionDocument> findByUserIdOrderByLastMessageAtDesc(String userId);

    boolean existsBySessionId(String sessionId);
}
