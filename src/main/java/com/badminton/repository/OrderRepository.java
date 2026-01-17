// backend/src/main/java/com/badminton/repository/OrderRepository.java
package com.badminton.repository;

import com.badminton.entity.Order;
import com.badminton.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByMomoOrderId(String momoOrderId);

    boolean existsByOrderNumber(String orderNumber);

    // Tìm theo user
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Tìm theo status
    Page<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);

    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Order.OrderStatus status, Pageable pageable);

    // Tìm theo payment status
    Page<Order> findByPaymentStatusOrderByCreatedAtDesc(Order.PaymentStatus paymentStatus, Pageable pageable);

    // Đơn hàng cần xử lý
    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'CONFIRMED', 'PROCESSING') " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findPendingOrders(Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('PENDING', 'CONFIRMED', 'PROCESSING')")
    Long countPendingOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'SHIPPING'")
    Long countShippingOrders();

    // Thống kê doanh thu
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'PAID'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'PAID' " +
            "AND o.paidAt >= :startDate AND o.paidAt <= :endDate")
    BigDecimal calculateRevenueByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'PAID' " +
            "AND DATE(o.paidAt) = CURRENT_DATE")
    BigDecimal calculateTodayRevenue();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'PAID' " +
            "AND MONTH(o.paidAt) = MONTH(CURRENT_DATE) " +
            "AND YEAR(o.paidAt) = YEAR(CURRENT_DATE)")
    BigDecimal calculateMonthRevenue();

    // Đếm đơn hàng
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countOrdersByUserId(Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status = :status")
    Long countOrdersByUserIdAndStatus(Long userId, Order.OrderStatus status);

    // Tìm đơn hàng để review
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId " +
            "AND o.status = 'DELIVERED' " +
            "AND o.deliveredAt IS NOT NULL " +
            "ORDER BY o.deliveredAt DESC")
    List<Order> findDeliveredOrdersForReview(@Param("userId") Long userId);

    // Kiểm tra user đã mua sản phẩm chưa
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END " +
            "FROM OrderItem oi WHERE oi.order.user.id = :userId " +
            "AND oi.product.id = :productId " +
            "AND oi.order.status = 'DELIVERED'")
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
