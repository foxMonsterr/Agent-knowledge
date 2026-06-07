package com.chat.myAgent.learn.repository.mongo;

import com.chat.myAgent.learn.model.FlashcardDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends MongoRepository<FlashcardDocument, String> {

    Optional<FlashcardDocument> findByCardIdAndUserId(String cardId, String userId);

    List<FlashcardDocument> findByUserIdAndNoteIdOrderByCreatedAtDesc(String userId, String noteId);

    List<FlashcardDocument> findByUserIdAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(String userId, LocalDateTime dueAt);

    List<FlashcardDocument> findByUserIdOrderByUpdatedAtDesc(String userId);

    long countByUserId(String userId);
}
