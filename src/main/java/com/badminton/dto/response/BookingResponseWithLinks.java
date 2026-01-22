package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import com.badminton.controller.AuthController;
import com.badminton.controller.BookingController;
import com.badminton.controller.CourtController;
import com.badminton.controller.PaymentController;
import com.badminton.controller.QRCodeController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponseWithLinks extends RepresentationModel<BookingResponseWithLinks> {

        private Long id;
        private Long userId;
        private String userName;
        private Long courtId;
        private String courtName;
        private LocalDate bookingDate;
        private String startTime;
        private String endTime;
        private BigDecimal totalPrice;
        private String status;
        private LocalDateTime createdAt;

        /**
         * Thêm HATEOAS links dựa trên trạng thái booking
         */
        public void addLinks(Long userId, String userRole) {
                // Self link
                add(linkTo(methodOn(BookingController.class)
                                .getBookingById(this.id, null))
                                .withSelfRel());

                // Court details link
                add(linkTo(methodOn(CourtController.class)
                                .getCourtById(this.courtId))
                                .withRel("court"));

                // User profile link
                add(linkTo(methodOn(AuthController.class)
                                .getProfile(null))
                                .withRel("user"));

                // Conditional links based on status
                if ("PENDING".equals(this.status)) {
                        // Cancel link (if user owns booking)
                        if (this.userId.equals(userId)) {
                                add(linkTo(methodOn(BookingController.class)
                                                .cancelBooking(this.id, null))
                                                .withRel("cancel"));
                        }

                        // Confirm link (if owner/admin)
                        if ("OWNER".equals(userRole) || "ADMIN".equals(userRole)) {
                                add(linkTo(methodOn(BookingController.class)
                                                .updateBookingStatus(this.id, "CONFIRMED", null))
                                                .withRel("confirm"));
                        }

                        // Payment link
                        add(linkTo(methodOn(PaymentController.class)
                                        .getPaymentByBooking(this.id))
                                        .withRel("payment"));
                }

                // QR code link
                add(linkTo(methodOn(QRCodeController.class)
                                .generateBookingQR(this.id, "DEPOSIT", null))
                                .withRel("qr-code"));
        }
}
