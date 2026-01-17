// backend/src/main/java/com/badminton/dto/response/ProductDetailResponse.java
package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private Integer stockQuantity;
    private Integer soldQuantity;
    private Long categoryId;
    private String categoryName;
    private List<String> images;
    private String brand;
    private Map<String, String> specifications;
    private String status;
    private Boolean featured;
    private Double averageRating;
    private Integer reviewCount;
    private List<ProductReviewResponse> recentReviews; // 5 review mới nhất
    private List<ProductResponse> relatedProducts; // Sản phẩm liên quan
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
