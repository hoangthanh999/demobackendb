package com.badminton.service;

import com.badminton.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {
    PaymentResponse getPaymentById(Long id);

    PaymentResponse getPaymentByBookingId(Long bookingId);

    // ✅ THÊM MỚI
    PaymentResponse getPaymentByOrderId(String orderId);

    Page<PaymentResponse> getAllPayments(Pageable pageable);

    Page<PaymentResponse> getPaymentsByStatus(String status, Pageable pageable);

    List<PaymentResponse> getPendingPayments();
}
