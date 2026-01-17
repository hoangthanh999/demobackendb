// backend/src/main/java/com/badminton/dto/response/ProductCategoryResponse.java
package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Boolean active;
    private Integer displayOrder;
    private Integer productCount; // Số lượng sản phẩm trong danh mục
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
