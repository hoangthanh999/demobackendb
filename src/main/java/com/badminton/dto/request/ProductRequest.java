// backend/src/main/java/com/badminton/dto/request/ProductRequest.java
package com.badminton.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 3, max = 200, message = "Tên sản phẩm phải từ 3-200 ký tự")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Giá gốc không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá gốc phải lớn hơn 0")
    private BigDecimal originalPrice;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    private List<String> images;

    @NotBlank(message = "Thương hiệu không được để trống")
    private String brand;

    private Map<String, String> specifications; // {"weight": "85g", "balance": "Head Heavy"}

    private Boolean featured;
}
