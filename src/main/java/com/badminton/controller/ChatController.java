// backend/src/main/java/com/badminton/controller/ChatController.java
package com.badminton.controller;

import com.badminton.dto.request.ChatRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.ChatResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @PostMapping("/message")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {

        log.info("💬 Received chat message: {}", request.getMessage());

        User user = getUserFromAuth(authentication);
        ChatResponse response = chatService.processMessage(user.getId(), request);

        return ResponseEntity.ok(ApiResponse.success(response, "Đã xử lý tin nhắn"));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ChatResponse>>> getChatHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = getUserFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChatResponse> history = chatService.getChatHistory(user.getId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @DeleteMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> clearSession(
            @PathVariable String sessionId,
            Authentication authentication) {

        User user = getUserFromAuth(authentication);
        chatService.clearSession(user.getId(), sessionId);

        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa session"));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
