package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userPhone;
    private Long courtId;
    private String courtName;
    private String courtAddress;
    private LocalDate bookingDate;
    private String startTime;
    private String endTime;
    private Integer courtNumber;
    private BigDecimal totalPrice;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
}
