// backend/src/main/java/com/badminton/controller/OrderController.java
package com.badminton.controller;

import com.badminton.dto.request.CreateOrderRequest;
import com.badminton.dto.request.UpdateOrderStatusRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.OrderResponse;
import com.badminton.dto.response.ShopStatisticsResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shop/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    // ==================== USER ENDPOINTS ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        OrderResponse order = orderService.createOrder(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(order, "Đặt hàng thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        OrderResponse order = orderService.getOrderById(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(
            @PathVariable String orderNumber,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber, user.getId());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getUserOrders(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/my-orders/status/{status}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getUserOrdersByStatus(user.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        OrderResponse order = orderService.cancelOrder(id, user.getId(), reason);
        return ResponseEntity.ok(ApiResponse.success(order, "Đã hủy đơn hàng"));
    }

    @GetMapping("/for-review")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersForReview(
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        List<OrderResponse> orders = orderService.getDeliveredOrdersForReview(user.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Cập nhật trạng thái thành công"));
    }

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShopStatisticsResponse>> getStatistics() {
        ShopStatisticsResponse stats = orderService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
