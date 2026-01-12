package com.badminton.service;

import com.badminton.dto.request.MoMoWebhookRequest;
import com.badminton.dto.request.PaymentRequest;
import com.badminton.dto.response.MoMoPaymentResponse;

public interface MoMoService {
    MoMoPaymentResponse createPayment(PaymentRequest request, Long userId);

    void handleWebhook(MoMoWebhookRequest webhook);

    boolean verifySignature(MoMoWebhookRequest webhook);
}
