// backend/src/main/java/com/badminton/dto/request/ProductReviewRequest.java
package com.badminton.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewRequest {

    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "ID đơn hàng không được để trống")
    private Long orderId;

    @NotNull(message = "Đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    private Integer rating;

    @NotBlank(message = "Nội dung đánh giá không được để trống")
    @Size(min = 10, max = 1000, message = "Nội dung đánh giá phải từ 10-1000 ký tự")
    private String comment;

    private List<String> images; // URLs của ảnh đánh giá
}
