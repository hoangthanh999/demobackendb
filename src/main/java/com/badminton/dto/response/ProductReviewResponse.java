// backend/src/main/java/com/badminton/dto/response/ProductReviewResponse.java
package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long orderId;
    private Integer rating;
    private String comment;
    private List<String> images;
    private Boolean verified; // Đã mua hàng
    private LocalDateTime createdAt;
}
