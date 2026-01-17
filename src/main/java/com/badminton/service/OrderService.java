// backend/src/main/java/com/badminton/service/OrderService.java
package com.badminton.service;

import com.badminton.dto.request.CreateOrderRequest;
import com.badminton.dto.request.UpdateOrderStatusRequest;
import com.badminton.dto.response.OrderResponse;
import com.badminton.dto.response.ShopStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    OrderResponse getOrderById(Long orderId, Long userId);

    OrderResponse getOrderByOrderNumber(String orderNumber, Long userId);

    Page<OrderResponse> getUserOrders(Long userId, Pageable pageable);

    Page<OrderResponse> getUserOrdersByStatus(Long userId, String status, Pageable pageable);

    Page<OrderResponse> getAllOrders(Pageable pageable);

    Page<OrderResponse> getOrdersByStatus(String status, Pageable pageable);

    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    OrderResponse cancelOrder(Long orderId, Long userId, String reason);

    void confirmPayment(String momoOrderId);

    ShopStatisticsResponse getStatistics();

    List<OrderResponse> getDeliveredOrdersForReview(Long userId);
}
