// backend/src/main/java/com/badminton/service/impl/OrderServiceImpl.java
package com.badminton.service.impl;

import com.badminton.dto.request.CreateOrderRequest;
import com.badminton.dto.request.UpdateOrderStatusRequest;
import com.badminton.dto.response.OrderItemResponse;
import com.badminton.dto.response.OrderResponse;
import com.badminton.dto.response.ShopStatisticsResponse;
import com.badminton.entity.*;
import com.badminton.service.UserTierService;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.exception.UnauthorizedException;
import com.badminton.repository.*;
import com.badminton.service.CartService;
import com.badminton.service.OrderService;
import com.badminton.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final UserTierService userTierService;

    private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(30000); // 30,000 VND
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(500000); // 500,000 VND

    @Override
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("Creating order for user {}", userId);

        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Validate items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải có ít nhất 1 sản phẩm");
        }

        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingProvince(request.getShippingProvince());
        order.setShippingDistrict(request.getShippingDistrict());
        order.setShippingWard(request.getShippingWard());
        order.setNote(request.getNote());
        order.setPaymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod().name()));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.UNPAID);

        // Process order items
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy sản phẩm ID: " + itemRequest.getProductId()));

            // Validate product
            if (product.getStatus() != Product.ProductStatus.ACTIVE) {
                throw new BadRequestException("Sản phẩm " + product.getName() + " hiện không khả dụng");
            }

            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new BadRequestException("Sản phẩm " + product.getName()
                        + " không đủ số lượng. Còn lại: " + product.getStockQuantity());
            }

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));

            orderItems.add(orderItem);
            subtotal = subtotal.add(orderItem.getSubtotal());

            // Update product stock and sold quantity
            productService.updateStock(product.getId(), itemRequest.getQuantity(), false);
            product.setSoldQuantity(product.getSoldQuantity() + itemRequest.getQuantity());
            productRepository.save(product);
        }

        order.setItems(orderItems);
        order.setSubtotal(subtotal);

        // Calculate shipping fee
        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : SHIPPING_FEE;
        order.setShippingFee(shippingFee);

        // Apply discount if coupon provided
        // TODO: Implement coupon logic
        order.setDiscount(BigDecimal.ZERO);

        // Calculate total
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(order.getDiscount());
        order.setTotalAmount(totalAmount);

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Clear cart after successful order
        try {
            cartService.clearCart(userId);
        } catch (Exception e) {
            log.warn("Failed to clear cart after order creation", e);
        }

        log.info("Order created successfully: {}", savedOrder.getOrderNumber());

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // Check permission
        if (!order.getUser().getId().equals(userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

            if (user.getRole() != User.UserRole.ADMIN) {
                throw new UnauthorizedException("Bạn không có quyền xem đơn hàng này");
            }
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // Check permission
        if (!order.getUser().getId().equals(userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

            if (user.getRole() != User.UserRole.ADMIN) {
                throw new UnauthorizedException("Bạn không có quyền xem đơn hàng này");
            }
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrdersByStatus(Long userId, String status, Pageable pageable) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, orderStatus, pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(String status, Pageable pageable) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        return orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus, pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to {}", orderId, request.getStatus());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(request.getStatus().toUpperCase());
        Order.OrderStatus currentStatus = order.getStatus();

        // Validate status transition
        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);

        // Update timestamps based on status
        switch (newStatus) {
            case SHIPPING:
                order.setShippedAt(LocalDateTime.now());
                break;
            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
                    order.setPaymentStatus(Order.PaymentStatus.PAID);
                    order.setPaidAt(LocalDateTime.now());
                }
                break;
            case CANCELLED:
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelReason(request.getCancelReason());
                // Restore product stock
                restoreProductStock(order);
                break;
        }

        Order updated = orderRepository.save(order);
        log.info("Order status updated successfully");

        return mapToOrderResponse(updated);
    }

    @Override
    public OrderResponse cancelOrder(Long orderId, Long userId, String reason) {
        log.info("User {} cancelling order {}", userId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // Check permission
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền hủy đơn hàng này");
        }

        // Check if order can be cancelled
        if (order.getStatus() == Order.OrderStatus.SHIPPING
                || order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("Không thể hủy đơn hàng đang giao hoặc đã giao");
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng đã được hủy trước đó");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason);

        // Restore product stock
        restoreProductStock(order);

        Order updated = orderRepository.save(order);
        log.info("Order cancelled successfully");

        return mapToOrderResponse(updated);
    }

    @Override
    public void confirmPayment(String momoOrderId) {
        log.info("Confirming payment for MoMo order: {}", momoOrderId);

        Order order = orderRepository.findByMomoOrderId(momoOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.CONFIRMED);

        orderRepository.save(order);

        // ✅ THÊM: Update user spending and tier
        userTierService.addSpending(order.getUser().getId(), order.getTotalAmount());

        log.info("Payment confirmed successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ShopStatisticsResponse getStatistics() {
        Long totalProducts = productRepository.countActiveProducts();
        Long totalOrders = orderRepository.count();
        Long pendingOrders = orderRepository.countPendingOrders();
        Long shippingOrders = orderRepository.countShippingOrders();

        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        BigDecimal todayRevenue = orderRepository.calculateTodayRevenue();
        BigDecimal monthRevenue = orderRepository.calculateMonthRevenue();

        Integer lowStockProducts = productRepository.countLowStockProducts();
        Integer outOfStockProducts = productRepository.countOutOfStockProducts();

        return ShopStatisticsResponse.builder()
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .shippingOrders(shippingOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .monthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .lowStockProducts(lowStockProducts)
                .outOfStockProducts(outOfStockProducts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getDeliveredOrdersForReview(Long userId) {
        return orderRepository.findDeliveredOrdersForReview(userId).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    private void validateStatusTransition(Order.OrderStatus current, Order.OrderStatus newStatus) {
        // Define valid transitions
        switch (current) {
            case PENDING:
                if (newStatus != Order.OrderStatus.CONFIRMED
                        && newStatus != Order.OrderStatus.CANCELLED) {
                    throw new BadRequestException("Chỉ có thể chuyển sang trạng thái XÁC NHẬN hoặc HỦY");
                }
                break;
            case CONFIRMED:
                if (newStatus != Order.OrderStatus.PROCESSING
                        && newStatus != Order.OrderStatus.CANCELLED) {
                    throw new BadRequestException("Chỉ có thể chuyển sang trạng thái ĐANG XỬ LÝ hoặc HỦY");
                }
                break;
            case PROCESSING:
                if (newStatus != Order.OrderStatus.SHIPPING
                        && newStatus != Order.OrderStatus.CANCELLED) {
                    throw new BadRequestException("Chỉ có thể chuyển sang trạng thái ĐANG GIAO hoặc HỦY");
                }
                break;
            case SHIPPING:
                if (newStatus != Order.OrderStatus.DELIVERED) {
                    throw new BadRequestException("Chỉ có thể chuyển sang trạng thái ĐÃ GIAO");
                }
                break;
            case DELIVERED:
                if (newStatus != Order.OrderStatus.RETURNED) {
                    throw new BadRequestException("Chỉ có thể chuyển sang trạng thái TRẢ HÀNG");
                }
                break;
            case CANCELLED:
            case RETURNED:
                throw new BadRequestException("Không thể thay đổi trạng thái đơn hàng này");
        }
    }

    private void restoreProductStock(Order order) {
        for (OrderItem item : order.getItems()) {
            try {
                productService.updateStock(item.getProduct().getId(), item.getQuantity(), true);

                Product product = item.getProduct();
                product.setSoldQuantity(Math.max(0, product.getSoldQuantity() - item.getQuantity()));
                productRepository.save(product);

                log.info("Restored stock for product {}: +{}", product.getId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to restore stock for product {}", item.getProduct().getId(), e);
            }
        }
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return "ORD_" + date + "_" + timestamp;
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userName(order.getUser().getFullName())
                .userPhone(order.getUser().getPhone())
                .items(items)
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentStatus().name())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .shippingProvince(order.getShippingProvince())
                .shippingDistrict(order.getShippingDistrict())
                .shippingWard(order.getShippingWard())
                .note(order.getNote())
                .momoTransactionId(order.getMomoTransactionId())
                .momoOrderId(order.getMomoOrderId())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .cancelReason(order.getCancelReason())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        String productImage = null;
        try {
            if (item.getProduct().getImages() != null) {
                List<String> images = objectMapper.readValue(item.getProduct().getImages(),
                        new TypeReference<List<String>>() {
                        });
                if (!images.isEmpty()) {
                    productImage = images.get(0);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing product images", e);
        }

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .productImage(productImage)
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
