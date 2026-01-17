// backend/src/main/java/com/badminton/dto/response/ShopStatisticsResponse.java
package com.badminton.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopStatisticsResponse {
    private Long totalProducts;
    private Long totalOrders;
    private Long pendingOrders;
    private Long shippingOrders;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    private Integer lowStockProducts; // Sản phẩm sắp hết hàng
    private Integer outOfStockProducts;
}
