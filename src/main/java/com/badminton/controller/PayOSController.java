package com.badminton.controller;

import com.badminton.dto.response.PayOSPaymentResponse;
import com.badminton.service.PayOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments/payos")
@RequiredArgsConstructor
@Slf4j
public class PayOSController {

    private final PayOSService payOSService;

    @PostMapping("/create-order/{orderId}")
    public ResponseEntity<PayOSPaymentResponse> createOrderPayment(@PathVariable Long orderId) {
        log.info("🔵 Creating PayOS payment for order: {}", orderId);
        PayOSPaymentResponse response = payOSService.createOrderPayment(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> webhookData) {
        log.info("📥 Received PayOS webhook");
        payOSService.handleWebhook(webhookData);
        return ResponseEntity.ok().build();
    }
}
