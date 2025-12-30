package com.badminton.repository;

import com.badminton.entity.Payment;
import com.badminton.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(Booking booking);

    Optional<Payment> findByTransactionId(String transactionId);
}
