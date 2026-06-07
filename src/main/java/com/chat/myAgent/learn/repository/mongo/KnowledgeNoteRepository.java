package com.chat.myAgent.learn.repository.mongo;

import com.chat.myAgent.learn.model.KnowledgeNoteDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeNoteRepository extends MongoRepository<KnowledgeNoteDocument, String> {

    Optional<KnowledgeNoteDocument> findByNoteIdAndUserId(String noteId, String userId);

    List<KnowledgeNoteDocument> findByUserIdAndArchivedOrderByUpdatedAtDesc(String userId, Boolean archived);

    List<KnowledgeNoteDocument> findByUserIdOrderByUpdatedAtDesc(String userId);

    long countByUserIdAndArchived(String userId, Boolean archived);

    long countByUserIdAndMasteryLevelGreaterThanEqualAndArchived(String userId, Integer masteryLevel, Boolean archived);

    long countByUserIdAndMasteryLevelLessThanAndArchived(String userId, Integer masteryLevel, Boolean archived);

    boolean existsByNoteId(String noteId);
}
