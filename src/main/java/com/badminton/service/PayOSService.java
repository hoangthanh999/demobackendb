package com.badminton.service;

import com.badminton.dto.response.PayOSPaymentResponse;

import java.util.Map;

public interface PayOSService {
    PayOSPaymentResponse createOrderPayment(Long orderId);

    void handleWebhook(Map<String, Object> webhookData);
}
