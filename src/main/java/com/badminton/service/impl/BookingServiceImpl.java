package com.badminton.service.impl;

import com.badminton.dto.request.BookingRequest;
import com.badminton.dto.response.BookingResponse;
import com.badminton.entity.Booking;
import com.badminton.entity.Court;
import com.badminton.entity.Payment;
import com.badminton.entity.User;

import com.badminton.service.UserTierService;
import com.badminton.repository.PaymentRepository;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.exception.UnauthorizedException;
import com.badminton.repository.BookingRepository;
import com.badminton.repository.CourtRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

        private final BookingRepository bookingRepository;
        private final CourtRepository courtRepository;
        private final UserRepository userRepository;
        private final PaymentRepository paymentRepository;
        private final UserTierService userTierService;

        @Override
        public BookingResponse createBooking(BookingRequest request, Long userId) {
                log.info("🔵 Creating booking for user: {}", userId);
                log.info("  Court ID: {}", request.getCourtId());
                log.info("  Date: {}", request.getBookingDate());
                log.info("  Start Time: {}", request.getStartTime());
                log.info("  End Time: {}", request.getEndTime());
                log.info("  Court Number: {}", request.getCourtNumber());

                // Validate user
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

                // Validate court
                Court court = courtRepository.findById(request.getCourtId())
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));

                log.info("  Court Name: {}", court.getName());
                log.info("  Court Open Time: {}", court.getOpenTime());
                log.info("  Court Close Time: {}", court.getCloseTime());

                // Check court status
                if (court.getStatus() != Court.CourtStatus.ACTIVE) {
                        throw new BadRequestException("Sân hiện không hoạt động");
                }

                // ✅ Parse times với error handling tốt hơn
                LocalTime startTime;
                LocalTime endTime;
                LocalTime openTime;
                LocalTime closeTime;

                try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

                        startTime = LocalTime.parse(request.getStartTime(), formatter);
                        endTime = LocalTime.parse(request.getEndTime(), formatter);
                        openTime = LocalTime.parse(court.getOpenTime(), formatter);
                        closeTime = LocalTime.parse(court.getCloseTime(), formatter);

                        log.info("✅ Parsed times successfully:");
                        log.info("  Start: {}", startTime);
                        log.info("  End: {}", endTime);
                        log.info("  Open: {}", openTime);
                        log.info("  Close: {}", closeTime);

                } catch (DateTimeParseException e) {
                        log.error("❌ Time parsing error: {}", e.getMessage());
                        throw new BadRequestException(
                                        String.format("Format thời gian không hợp lệ. Vui lòng sử dụng format HH:mm (VD: 08:00). "
                                                        +
                                                        "Bạn đã nhập: Start=%s, End=%s",
                                                        request.getStartTime(), request.getEndTime()));
                }

                // ✅ Validate booking date
                LocalDate bookingDate = request.getBookingDate();
                LocalDate today = LocalDate.now();

                if (bookingDate.isBefore(today)) {
                        throw new BadRequestException("Không thể đặt sân cho ngày trong quá khứ");
                }

                // ✅ Validate time logic
                if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
                        throw new BadRequestException(
                                        String.format("Thời gian bắt đầu (%s) phải trước thời gian kết thúc (%s)",
                                                        request.getStartTime(), request.getEndTime()));
                }

                // ✅ Validate court operating hours - CHI TIẾT HƠN
                if (startTime.isBefore(openTime)) {
                        throw new BadRequestException(
                                        String.format("⏰ Sân chỉ mở cửa từ %s. Giờ bắt đầu của bạn (%s) quá sớm.",
                                                        court.getOpenTime(), request.getStartTime()));
                }

                if (endTime.isAfter(closeTime)) {
                        throw new BadRequestException(
                                        String.format("⏰ Sân đóng cửa lúc %s. Giờ kết thúc của bạn (%s) quá muộn.",
                                                        court.getCloseTime(), request.getEndTime()));
                }

                // ✅ Validate nếu thời gian nằm ngoài khung giờ hoạt động
                if (startTime.isBefore(openTime) || endTime.isAfter(closeTime)) {
                        throw new BadRequestException(
                                        String.format("⏰ Sân chỉ mở cửa từ %s đến %s. " +
                                                        "Thời gian đặt của bạn (%s - %s) không hợp lệ.",
                                                        court.getOpenTime(), court.getCloseTime(),
                                                        request.getStartTime(), request.getEndTime()));
                }

                // ✅ Validate court number
                if (request.getCourtNumber() < 1 || request.getCourtNumber() > court.getNumberOfCourts()) {
                        throw new BadRequestException(
                                        String.format("Số sân không hợp lệ. Sân này có %d sân (từ 1 đến %d). Bạn chọn: %d",
                                                        court.getNumberOfCourts(), court.getNumberOfCourts(),
                                                        request.getCourtNumber()));
                }

                // ✅ Check for conflicts
                List<Booking> conflicts = bookingRepository.findConflictingBookings(
                                court.getId(),
                                bookingDate,
                                request.getCourtNumber(),
                                startTime,
                                endTime);

                if (!conflicts.isEmpty()) {
                        log.warn("❌ Found {} conflicting bookings", conflicts.size());
                        throw new BadRequestException(
                                        String.format("Sân số %d đã được đặt trong khung giờ %s - %s ngày %s",
                                                        request.getCourtNumber(),
                                                        request.getStartTime(),
                                                        request.getEndTime(),
                                                        bookingDate));
                }

                boolean isVIP = userTierService.canBookWithoutDeposit(user);
                Integer depositPercentage = userTierService.getDepositPercentage(user);

                log.info("👤 User tier: {}, Deposit: {}%, VIP: {}",
                                user.getTier(), depositPercentage, isVIP);

                // ✅ Calculate total price
                long hours = Duration.between(startTime, endTime).toHours();
                if (hours < 1) {
                        throw new BadRequestException("Thời gian đặt sân tối thiểu là 1 giờ");
                }

                BigDecimal totalPrice = court.getPricePerHour().multiply(BigDecimal.valueOf(hours));
                log.info("💰 Total price: {} VND ({} hours x {} VND)", totalPrice, hours, court.getPricePerHour());

                // ✅ Create booking
                Booking booking = new Booking();
                booking.setUser(user);
                booking.setCourt(court);
                booking.setBookingDate(bookingDate);
                booking.setStartTime(startTime);
                booking.setEndTime(endTime);
                booking.setCourtNumber(request.getCourtNumber());
                booking.setTotalPrice(totalPrice);
                booking.setNotes(request.getNotes());
                booking.setStatus(Booking.BookingStatus.PENDING);

                Booking savedBooking = bookingRepository.save(booking);

                Payment payment = Payment.builder()
                                .booking(savedBooking)
                                .amount(totalPrice)
                                .depositAmount(totalPrice.multiply(BigDecimal.valueOf(depositPercentage))
                                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP))
                                .remainingAmount(totalPrice.multiply(BigDecimal.valueOf(100 - depositPercentage))
                                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP))
                                .paymentMethod(Payment.PaymentMethod.MOMO)
                                .paymentType(Payment.PaymentType.DEPOSIT)
                                .status(isVIP ? Payment.PaymentStatus.COMPLETED : Payment.PaymentStatus.PENDING)
                                .expiredAt(LocalDateTime.now().plusMinutes(15))
                                .build();

                if (isVIP) {
                        payment.setPaidAt(LocalDateTime.now());
                        payment.setTransactionId("VIP_AUTO_" + System.currentTimeMillis());
                        savedBooking.setStatus(Booking.BookingStatus.CONFIRMED);
                        log.info("👑 VIP booking auto-confirmed without deposit");
                }

                paymentRepository.save(payment);

                log.info("✅ Booking created successfully with ID: {}", savedBooking.getId());

                return mapToBookingResponse(savedBooking);
        }

        @Override
        public BookingResponse getBookingById(Long id, Long userId) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân"));

                if (!booking.getUser().getId().equals(userId) &&
                                !booking.getCourt().getOwner().getId().equals(userId)) {
                        throw new UnauthorizedException("Bạn không có quyền xem đặt sân này");
                }

                return mapToBookingResponse(booking);
        }

        @Override
        public Page<BookingResponse> getUserBookings(Long userId, Pageable pageable) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

                return bookingRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                                .map(this::mapToBookingResponse);
        }

        @Override
        public Page<BookingResponse> getOwnerBookings(Long ownerId, Pageable pageable) {
                return bookingRepository.findByCourtOwnerId(ownerId, pageable)
                                .map(this::mapToBookingResponse);
        }

        @Override
        public BookingResponse updateBookingStatus(Long id, String status, Long userId) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

                boolean isOwner = booking.getCourt().getOwner().getId().equals(userId);
                boolean isAdmin = user.getRole() == User.UserRole.ADMIN;

                if (!isOwner && !isAdmin) {
                        throw new UnauthorizedException("Bạn không có quyền cập nhật trạng thái đặt sân này");
                }

                booking.setStatus(Booking.BookingStatus.valueOf(status.toUpperCase()));
                Booking updatedBooking = bookingRepository.save(booking);
                return mapToBookingResponse(updatedBooking);
        }

        @Override
        public void cancelBooking(Long id, Long userId) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

                boolean isBookingOwner = booking.getUser().getId().equals(userId);
                boolean isCourtOwner = booking.getCourt().getOwner().getId().equals(userId);
                boolean isAdmin = user.getRole() == User.UserRole.ADMIN;

                if (!isBookingOwner && !isCourtOwner && !isAdmin) {
                        throw new UnauthorizedException("Bạn không có quyền hủy đặt sân này");
                }

                if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
                        throw new BadRequestException("Không thể hủy đặt sân đã hoàn thành");
                }

                booking.setStatus(Booking.BookingStatus.CANCELLED);
                bookingRepository.save(booking);
        }

        @Override
        public List<BookingResponse> getCourtBookings(Long courtId, Long ownerId) {
                Court court = courtRepository.findById(courtId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));

                User user = userRepository.findById(ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

                boolean isOwner = court.getOwner().getId().equals(ownerId);
                boolean isAdmin = user.getRole() == User.UserRole.ADMIN;

                if (!isOwner && !isAdmin) {
                        throw new UnauthorizedException("Bạn không có quyền xem đặt sân của sân này");
                }

                return bookingRepository.findByCourt(court).stream()
                                .map(this::mapToBookingResponse)
                                .collect(Collectors.toList());
        }

        @Override
        public Page<BookingResponse> getAllBookings(Pageable pageable) {
                return bookingRepository.findAll(pageable)
                                .map(this::mapToBookingResponse);
        }

        private BookingResponse mapToBookingResponse(Booking booking) {
                return BookingResponse.builder()
                                .id(booking.getId())
                                .userId(booking.getUser().getId())
                                .userName(booking.getUser().getFullName())
                                .userPhone(booking.getUser().getPhone())
                                .courtId(booking.getCourt().getId())
                                .courtName(booking.getCourt().getName())
                                .courtAddress(booking.getCourt().getAddress())
                                .bookingDate(booking.getBookingDate())
                                .startTime(booking.getStartTime().toString())
                                .endTime(booking.getEndTime().toString())
                                .courtNumber(booking.getCourtNumber())
                                .totalPrice(booking.getTotalPrice())
                                .status(booking.getStatus().name())
                                .notes(booking.getNotes())
                                .createdAt(booking.getCreatedAt())
                                .build();
        }
}
