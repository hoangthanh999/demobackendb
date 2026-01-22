// backend/src/main/java/com/badminton/service/ChatService.java
package com.badminton.service;

import com.badminton.dto.request.ChatRequest;
import com.badminton.dto.response.ChatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatService {
    ChatResponse processMessage(Long userId, ChatRequest request);

    Page<ChatResponse> getChatHistory(Long userId, Pageable pageable);

    void clearSession(Long userId, String sessionId);
}
