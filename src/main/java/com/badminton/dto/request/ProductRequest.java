// backend/src/main/java/com/badminton/dto/request/ProductRequest.java
package com.badminton.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 3, max = 200, message = "Tên sản phẩm phải từ 3-200 ký tự")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    private BigDecimal price;

    // Optional: if not provided, use price as originalPrice
    private BigDecimal originalPrice;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    private List<String> images;

    // Optional: default to "Unknown" if not provided
    private String brand;

    private Map<String, String> specifications; // {"weight": "85g", "balance": "Head Heavy"}

    private Boolean featured;

    private Boolean bestseller; // Frontend sends this

    private String warranty; // Frontend sends this (e.g., "12 tháng")

    // Custom setter to support both 'stock' (frontend) and 'stockQuantity'
    // (backend)
    public void setStock(Integer stock) {
        this.stockQuantity = stock;
    }

    // Getter for stock - returns stockQuantity
    public Integer getStock() {
        return this.stockQuantity;
    }
}
