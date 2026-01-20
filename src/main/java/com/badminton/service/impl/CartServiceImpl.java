// backend/src/main/java/com/badminton/service/impl/CartServiceImpl.java
package com.badminton.service.impl;

import com.badminton.dto.request.AddToCartRequest;
import com.badminton.dto.request.UpdateCartItemRequest;
import com.badminton.dto.response.CartItemResponse;
import com.badminton.dto.response.CartResponse;
import com.badminton.entity.Cart;
import com.badminton.entity.CartItem;
import com.badminton.entity.Product;
import com.badminton.entity.User;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.CartItemRepository;
import com.badminton.repository.CartRepository;
import com.badminton.repository.ProductRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.CartService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional // ✅ BỎ readOnly = true
    public CartResponse getCart(Long userId) {
        log.info("🛒 Getting cart for user: {}", userId);
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional // ✅ ĐÃ ĐÚNG
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        log.info("➕ Adding product {} to cart for user {}", request.getProductId(), userId);

        // Get or create cart
        Cart cart = getOrCreateCart(userId);

        // Validate product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // Check product status
        if (product.getStatus() != Product.ProductStatus.ACTIVE) {
            throw new BadRequestException("Sản phẩm hiện không khả dụng");
        }

        // Check stock
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Số lượng sản phẩm trong kho không đủ. Còn lại: "
                    + product.getStockQuantity());
        }

        // Check if product already in cart
        CartItem existingItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElse(null);

        if (existingItem != null) {
            // Update quantity
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (newQuantity > product.getStockQuantity()) {
                throw new BadRequestException("Tổng số lượng vượt quá tồn kho. Còn lại: "
                        + product.getStockQuantity());
            }

            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            log.info("✅ Updated cart item quantity to {}", newQuantity);
        } else {
            // Add new item
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(product.getPrice());

            cart.getItems().add(cartItem);
            cartItemRepository.save(cartItem);
            log.info("✅ Added new item to cart");
        }

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional // ✅ ĐÃ ĐÚNG
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        log.info("🔄 Updating cart item {} for user {}", cartItemId, userId);

        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng"));

        // Verify cart ownership
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Sản phẩm không thuộc giỏ hàng của bạn");
        }

        // Check stock
        Product product = cartItem.getProduct();
        if (request.getQuantity() > product.getStockQuantity()) {
            throw new BadRequestException("Số lượng sản phẩm trong kho không đủ. Còn lại: "
                    + product.getStockQuantity());
        }

        cartItem.setQuantity(request.getQuantity());
        cartItem.setPrice(product.getPrice()); // Update price in case it changed
        cartItemRepository.save(cartItem);

        log.info("✅ Cart item updated successfully");
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional // ✅ ĐÃ ĐÚNG
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        log.info("🗑️ Removing cart item {} for user {}", cartItemId, userId);

        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng"));

        // Verify cart ownership
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Sản phẩm không thuộc giỏ hàng của bạn");
        }

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        log.info("✅ Cart item removed successfully");
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional // ✅ ĐÃ ĐÚNG
    public void clearCart(Long userId) {
        log.info("🧹 Clearing cart for user {}", userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));

        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.getItems().clear();

        log.info("✅ Cart cleared successfully");
    }

    @Override
    @Transactional(readOnly = true) // ✅ Method này CHỈ ĐỌC nên OK
    public Integer getCartItemCount(Long userId) {
        return cartRepository.countItemsByUserId(userId);
    }

    // ✅ Method này CÓ THỂ INSERT → Không được gọi từ readOnly transaction
    private Cart getOrCreateCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info("📦 Creating new cart for user: {}", userId);
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    Cart savedCart = cartRepository.save(newCart);
                    log.info("✅ New cart created with ID: {}", savedCart.getId());
                    return savedCart;
                });
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalItems = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        Product product = item.getProduct();

        String productImage = null;
        try {
            if (product.getImages() != null) {
                List<String> images = objectMapper.readValue(product.getImages(),
                        new TypeReference<List<String>>() {
                        });
                if (!images.isEmpty()) {
                    productImage = images.get(0);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("❌ Error processing product images", e);
        }

        BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        boolean available = product.getStatus() == Product.ProductStatus.ACTIVE
                && product.getStockQuantity() >= item.getQuantity();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(productImage)
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .stockQuantity(product.getStockQuantity())
                .available(available)
                .build();
    }
}
