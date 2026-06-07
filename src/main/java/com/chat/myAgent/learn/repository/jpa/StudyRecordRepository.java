package com.chat.myAgent.learn.repository.jpa;

import com.chat.myAgent.learn.model.StudyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StudyRecordRepository extends JpaRepository<StudyRecordEntity, Long> {

    List<StudyRecordEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<StudyRecordEntity> findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(String userId, LocalDateTime since);

    long countByUserIdAndActivityType(String userId, String activityType);

    @Query("SELECT FUNCTION('HOUR', r.createdAt) as hour, COUNT(r) as cnt FROM StudyRecordEntity r WHERE r.userId = :userId GROUP BY FUNCTION('HOUR', r.createdAt) ORDER BY cnt DESC")
    List<Object[]> bestStudyHours(@Param("userId") String userId);

    @Query("SELECT r.activityType, COUNT(r) as cnt FROM StudyRecordEntity r WHERE r.userId = :userId GROUP BY r.activityType ORDER BY cnt DESC")
    List<Object[]> activityTypeDistribution(@Param("userId") String userId);

    @Query("SELECT r.topic, SUM(r.durationSeconds) as total FROM StudyRecordEntity r WHERE r.userId = :userId AND r.topic IS NOT NULL AND r.topic <> '' GROUP BY r.topic ORDER BY total DESC")
    List<Object[]> topicStudyTime(@Param("userId") String userId);
}
