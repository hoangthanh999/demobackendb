// backend/src/main/java/com/badminton/repository/OrderItemRepository.java
package com.badminton.repository;

import com.badminton.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId")
    List<OrderItem> findByProductId(Long productId);

    // Thống kê sản phẩm bán chạy
    @Query("SELECT oi.product.id, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi " +
            "WHERE oi.order.status = 'DELIVERED' " +
            "GROUP BY oi.product.id " +
            "ORDER BY totalSold DESC")
    List<Object[]> findBestSellingProducts(org.springframework.data.domain.Pageable pageable);

    // Kiểm tra user đã mua sản phẩm trong order cụ thể
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END " +
            "FROM OrderItem oi WHERE oi.order.id = :orderId " +
            "AND oi.product.id = :productId")
    boolean existsByOrderIdAndProductId(@Param("orderId") Long orderId,
            @Param("productId") Long productId);
}
