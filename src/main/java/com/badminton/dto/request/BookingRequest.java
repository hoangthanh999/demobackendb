package com.badminton.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull(message = "ID sân không được để trống")
    private Long courtId;

    @NotNull(message = "Ngày đặt sân không được để trống")
    @Future(message = "Ngày đặt sân phải là ngày trong tương lai")
    private LocalDate bookingDate;

    @NotBlank(message = "Giờ bắt đầu không được để trống")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ bắt đầu không hợp lệ (HH:mm)")
    private String startTime;

    @NotBlank(message = "Giờ kết thúc không được để trống")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ kết thúc không hợp lệ (HH:mm)")
    private String endTime;

    @NotNull(message = "Số sân không được để trống")
    @Min(value = 1, message = "Số sân phải lớn hơn 0")
    private Integer courtNumber;

    private String notes;
}
