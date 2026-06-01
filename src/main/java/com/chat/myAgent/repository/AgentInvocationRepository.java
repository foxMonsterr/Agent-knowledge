package com.chat.myAgent.repository;

import com.chat.myAgent.model.entity.AgentInvocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface AgentInvocationRepository extends JpaRepository<AgentInvocationEntity, Long> {

    List<AgentInvocationEntity> findByConversationIdOrderByCreatedAtDesc(String conversationId);

    List<AgentInvocationEntity> findTop8ByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(a) FROM AgentInvocationEntity a WHERE a.createdAt BETWEEN :start AND :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(AVG(COALESCE(a.latencyMs, 0)), 0) FROM AgentInvocationEntity a WHERE a.createdAt BETWEEN :start AND :end")
    double avgLatencyByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AgentInvocationEntity a WHERE a.createdAt BETWEEN :start AND :end AND UPPER(a.status) = UPPER(:status)")
    long countByCreatedAtBetweenAndStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("status") String status);

    long countByAgentType(String agentType);
}
