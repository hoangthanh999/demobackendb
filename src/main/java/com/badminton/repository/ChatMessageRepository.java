// backend/src/main/java/com/badminton/repository/ChatMessageRepository.java
package com.badminton.repository;

import com.badminton.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<ChatMessage> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
