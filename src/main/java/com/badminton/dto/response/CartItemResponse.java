// backend/src/main/java/com/badminton/dto/response/CartItemResponse.java
package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage; // Ảnh đầu tiên
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal; // price * quantity
    private Integer stockQuantity; // Số lượng còn trong kho
    private Boolean available; // Còn hàng hay không
}
