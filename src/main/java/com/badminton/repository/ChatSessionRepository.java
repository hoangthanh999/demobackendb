// backend/src/main/java/com/badminton/repository/ChatSessionRepository.java
package com.badminton.repository;

import com.badminton.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionIdAndStatus(String sessionId, ChatSession.SessionStatus status);

    List<ChatSession> findByUserIdAndStatus(Long userId, ChatSession.SessionStatus status);

    List<ChatSession> findByStatusAndExpiredAtBefore(ChatSession.SessionStatus status, LocalDateTime now);
}
