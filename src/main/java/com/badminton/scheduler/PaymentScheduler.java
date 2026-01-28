package com.badminton.scheduler;

import com.badminton.entity.Booking;
import com.badminton.entity.Payment;
import com.badminton.repository.BookingRepository;
import com.badminton.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    @Transactional
    public void cancelExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();

        List<Payment> expiredPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                .filter(p -> p.getExpiredAt() != null && p.getExpiredAt().isBefore(now))
                .toList();

        for (Payment payment : expiredPayments) {
            payment.setStatus(Payment.PaymentStatus.EXPIRED);
            payment.getBooking().setStatus(Booking.BookingStatus.CANCELLED);

            paymentRepository.save(payment);
            bookingRepository.save(payment.getBooking());

            log.info("Cancelled expired payment for booking: {}", payment.getBooking().getId());
        }
    }

    @Scheduled(fixedRate = 300000) // Chạy mỗi 5 phút
    @Transactional
    public void autoCompleteBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookingsToComplete = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .filter(b -> {
                    LocalDateTime endDateTime = LocalDateTime.of(b.getBookingDate(), b.getEndTime());
                    return endDateTime.isBefore(now);
                })
                .toList();

        for (Booking booking : bookingsToComplete) {
            booking.setStatus(Booking.BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            log.info("✅ Auto-completed booking ID: {} (Court: {}, Date: {}, Time: {} - {})",
                    booking.getId(),
                    booking.getCourt().getName(),
                    booking.getBookingDate(),
                    booking.getStartTime(),
                    booking.getEndTime());
        }

        if (!bookingsToComplete.isEmpty()) {
            log.info("🎯 Completed {} bookings", bookingsToComplete.size());
        }
    }
}
