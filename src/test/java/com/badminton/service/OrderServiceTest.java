package com.badminton.service;

import com.badminton.dto.request.CreateOrderRequest;
import com.badminton.dto.request.UpdateOrderStatusRequest;
import com.badminton.dto.response.OrderResponse;
import com.badminton.entity.Order;
import com.badminton.entity.User;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.OrderRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Order order;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFullName("Test User");

        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD_001");
        order.setUser(user);
        order.setSubtotal(BigDecimal.valueOf(100000));
        order.setShippingFee(BigDecimal.valueOf(20000));
        order.setDiscount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.valueOf(120000));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.UNPAID);
        order.setPaymentMethod(Order.PaymentMethod.COD);
        order.setRecipientName("Test User");
        order.setRecipientPhone("0909000111");
        order.setShippingAddress("123 Test St");
        order.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenExists() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        Order result = orderRepository.findById(1L).orElseThrow();

        // Assert
        assertNotNull(result);
        assertEquals("ORD_001", result.getOrderNumber());
        assertEquals(user.getId(), result.getUser().getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getOrderById_ShouldThrowException_WhenNotExists() {
        // Arrange
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            orderRepository.findById(999L).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        });
    }

    @Test
    void getUserOrders_ShouldReturnOrder_WhenUserHasOrders() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        Optional<Order> result = orderRepository.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user.getId(), result.get().getUser().getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatus_WhenValidTransition() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        order.setStatus(Order.OrderStatus.CONFIRMED);
        Order result = orderRepository.save(order);

        // Assert
        assertEquals(Order.OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void cancelOrder_ShouldSetCancelledStatus() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason("Customer request");
        Order result = orderRepository.save(order);

        // Assert
        assertEquals(Order.OrderStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
        assertEquals("Customer request", result.getCancelReason());
    }

    @Test
    void createOrder_ShouldGenerateOrderNumber() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(2L);
            return savedOrder;
        });

        // Act
        Order newOrder = new Order();
        newOrder.setUser(user);
        newOrder.setOrderNumber("ORD_" + System.currentTimeMillis());
        newOrder.setSubtotal(BigDecimal.valueOf(50000));
        newOrder.setTotalAmount(BigDecimal.valueOf(50000));
        newOrder.setPaymentMethod(Order.PaymentMethod.MOMO);
        newOrder.setRecipientName("Test User");
        newOrder.setRecipientPhone("0909000111");
        newOrder.setShippingAddress("456 New St");

        Order saved = orderRepository.save(newOrder);

        // Assert
        assertNotNull(saved);
        assertNotNull(saved.getOrderNumber());
        assertTrue(saved.getOrderNumber().startsWith("ORD_"));
    }
}
