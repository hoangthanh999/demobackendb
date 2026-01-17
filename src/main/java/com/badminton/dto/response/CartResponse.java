// backend/src/main/java/com/badminton/dto/response/CartResponse.java
package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {
    private Long id;
    private Long userId;
    private List<CartItemResponse> items;
    private Integer totalItems; // Tổng số sản phẩm
    private BigDecimal totalAmount; // Tổng tiền
    private LocalDateTime updatedAt;
}
