package com.badminton.controller;

import com.badminton.dto.request.MoMoWebhookRequest;
import com.badminton.dto.request.PaymentRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.MoMoPaymentResponse;
import com.badminton.dto.response.MoMoTransactionStatusResponse;
import com.badminton.dto.response.PaymentResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.MoMoService;
import com.badminton.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final MoMoService moMoService;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @PostMapping("/momo/create")

    public ResponseEntity<ApiResponse<MoMoPaymentResponse>> createMoMoPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        MoMoPaymentResponse response = moMoService.createPayment(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo thanh toán thành công"));
    }

    @PostMapping("/mock/confirm/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmMockPayment(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "0") int resultCode) {

        log.info("🎭 Mock payment confirmation: orderId={}, resultCode={}", orderId, resultCode);

        // Tạo mock webhook request
        MoMoWebhookRequest webhook = new MoMoWebhookRequest();
        webhook.setOrderId(orderId);
        webhook.setResultCode(resultCode);
        webhook.setTransId(System.currentTimeMillis());

        moMoService.handleWebhook(webhook);

        PaymentResponse payment = paymentService.getPaymentByBookingId(
                paymentService.getPaymentByOrderId(orderId).getBookingId());

        return ResponseEntity.ok(ApiResponse.success(payment,
                resultCode == 0 ? "Thanh toán thành công" : "Thanh toán thất bại"));
    }

    @PostMapping("/momo/webhook")
    public ResponseEntity<Void> handleMoMoWebhook(@RequestBody MoMoWebhookRequest webhook) {
        log.info("📥 Received MoMo webhook: {}", webhook);
        moMoService.handleWebhook(webhook);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByBooking(
            @PathVariable Long bookingId) {
        PaymentResponse payment = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PaymentResponse> payments = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentResponse> payments = paymentService.getPaymentsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPendingPayments() {
        List<PaymentResponse> payments = paymentService.getPendingPayments();
        return ResponseEntity.ok(ApiResponse.success(payments,
                "Có " + payments.size() + " thanh toán đang chờ xử lý"));
    }

    @GetMapping("/admin/check-momo-status/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MoMoTransactionStatusResponse>> checkMoMoStatus(
            @PathVariable String orderId) {
        MoMoTransactionStatusResponse status = moMoService.queryTransactionStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(status,
                "Trạng thái: " + status.getStatusDescription()));
    }

    @PostMapping("/admin/manual-confirm/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> manualConfirmPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String transactionId) {
        moMoService.manualConfirmPayment(paymentId, transactionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xác nhận thanh toán thành công"));
    }

    @GetMapping("/admin/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable Long paymentId) {
        PaymentResponse payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
