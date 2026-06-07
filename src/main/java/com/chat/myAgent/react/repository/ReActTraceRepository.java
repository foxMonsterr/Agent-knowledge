package com.chat.myAgent.react.repository;

import com.chat.myAgent.react.model.ReActTraceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReActTraceRepository extends MongoRepository<ReActTraceDocument, String> {

    Optional<ReActTraceDocument> findByTraceIdAndUserId(String traceId, String userId);

    List<ReActTraceDocument> findByUserIdAndSessionIdOrderByCreatedAtDesc(String userId, String sessionId);

    List<ReActTraceDocument> findByUserIdOrderByCreatedAtDesc(String userId);
}
