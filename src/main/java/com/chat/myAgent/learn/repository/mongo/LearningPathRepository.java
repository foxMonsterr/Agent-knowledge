package com.chat.myAgent.learn.repository.mongo;

import com.chat.myAgent.learn.model.LearningPathDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LearningPathRepository extends MongoRepository<LearningPathDocument, String> {

    Optional<LearningPathDocument> findByPathIdAndUserId(String pathId, String userId);

    List<LearningPathDocument> findByUserIdOrderByCreatedAtDesc(String userId);

    List<LearningPathDocument> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);
}
