// backend/src/main/java/com/badminton/dto/request/ProductSearchRequest.java
package com.badminton.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {
    private String keyword;
    private Long categoryId;
    private String brand;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean featured;
    private String sortBy; // price, name, soldQuantity, createdAt
    private String sortDir; // ASC, DESC
    private Integer page = 0;
    private Integer size = 20;
}
