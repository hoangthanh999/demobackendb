package com.badminton.service;

import com.badminton.dto.response.VNPayPaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface VNPayService {
    VNPayPaymentResponse createPaymentUrl(Long bookingId, String paymentType, HttpServletRequest request);

    VNPayPaymentResponse createOrderPaymentUrl(Long orderId, HttpServletRequest request);

    void handleCallback(Map<String, String> params);

    boolean verifySignature(Map<String, String> params);
}
