package com.chat.myAgent.repository;

import com.chat.myAgent.model.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, String> {

    List<ChatSessionEntity> findByUsernameOrderByLastMessageAtDesc(String username);
}
