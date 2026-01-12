package com.badminton.repository;

import com.badminton.entity.Payment;
import com.badminton.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(Booking booking);

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByRequestId(String requestId);

    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    // ✅ THÊM MỚI - Kiểm tra booking đã có payment chưa
    boolean existsByBooking(Booking booking);

    boolean existsByBookingId(Long bookingId);
}
