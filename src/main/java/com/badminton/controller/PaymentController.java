package com.badminton.controller;

import com.badminton.dto.request.MoMoWebhookRequest;
import com.badminton.dto.request.PaymentRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.MoMoPaymentResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.MoMoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final MoMoService moMoService;
    private final UserRepository userRepository;

    @PostMapping("/momo/create")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MoMoPaymentResponse>> createMoMoPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        MoMoPaymentResponse response = moMoService.createPayment(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo thanh toán thành công"));
    }

    @PostMapping("/momo/webhook")
    public ResponseEntity<Void> handleMoMoWebhook(@RequestBody MoMoWebhookRequest webhook) {
        log.info("Received MoMo webhook: {}", webhook);
        moMoService.handleWebhook(webhook);
        return ResponseEntity.ok().build();
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
