package com.badminton.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtRequest {

    @NotBlank(message = "Tên sân không được để trống")
    @Size(min = 3, max = 200, message = "Tên sân phải từ 3-200 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    private String description;

    @NotNull(message = "Giá thuê không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá thuê phải lớn hơn 0")
    private BigDecimal pricePerHour;

    @NotNull(message = "Số lượng sân không được để trống")
    @Min(value = 1, message = "Số lượng sân phải ít nhất là 1")
    private Integer numberOfCourts;

    private List<String> facilities;

    private List<String> images;

    @NotBlank(message = "Giờ mở cửa không được để trống")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ mở cửa không hợp lệ (HH:mm)")
    private String openTime;

    @NotBlank(message = "Giờ đóng cửa không được để trống")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ đóng cửa không hợp lệ (HH:mm)")
    private String closeTime;
}
