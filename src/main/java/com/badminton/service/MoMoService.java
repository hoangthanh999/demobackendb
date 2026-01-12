package com.badminton.service;

import com.badminton.dto.request.MoMoWebhookRequest;
import com.badminton.dto.request.PaymentRequest;
import com.badminton.dto.response.MoMoPaymentResponse;
import com.badminton.dto.response.MoMoTransactionStatusResponse;

public interface MoMoService {
    MoMoPaymentResponse createPayment(PaymentRequest request, Long userId);

    void handleWebhook(MoMoWebhookRequest webhook);

    boolean verifySignature(MoMoWebhookRequest webhook);

    MoMoTransactionStatusResponse queryTransactionStatus(String orderId);

    // ✅ THÊM MỚI: Admin xác nhận thủ công
    void manualConfirmPayment(Long paymentId, String transactionId);
}
