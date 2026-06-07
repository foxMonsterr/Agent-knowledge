package com.chat.myAgent.learn.repository.mongo;

import com.chat.myAgent.learn.model.QuizDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends MongoRepository<QuizDocument, String> {

    Optional<QuizDocument> findByQuizIdAndUserId(String quizId, String userId);

    List<QuizDocument> findByUserIdAndNoteIdOrderByCreatedAtDesc(String userId, String noteId);

    List<QuizDocument> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserId(String userId);
}
