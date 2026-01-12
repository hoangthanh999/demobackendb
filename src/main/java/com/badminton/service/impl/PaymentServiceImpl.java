package com.badminton.service.impl;

import com.badminton.dto.response.PaymentResponse;
import com.badminton.entity.Payment;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.PaymentRepository;
import com.badminton.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán"));
        return mapToPaymentResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán cho booking này"));
        return mapToPaymentResponse(payment);
    }

    // ✅ THÊM METHOD NÀY
    @Override
    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán với orderId: " + orderId));
        return mapToPaymentResponse(payment);
    }

    @Override
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(this::mapToPaymentResponse);
    }

    @Override
    public Page<PaymentResponse> getPaymentsByStatus(String status, Pageable pageable) {
        Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
        return paymentRepository.findByStatus(paymentStatus, pageable)
                .map(this::mapToPaymentResponse);
    }

    @Override
    public List<PaymentResponse> getPendingPayments() {
        return paymentRepository.findByStatus(Payment.PaymentStatus.PENDING).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .amount(payment.getAmount())
                .depositAmount(payment.getDepositAmount())
                .remainingAmount(payment.getRemainingAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .paymentType(payment.getPaymentType().name())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .orderId(payment.getOrderId())
                .requestId(payment.getRequestId())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .expiredAt(payment.getExpiredAt())
                .build();
    }
}
