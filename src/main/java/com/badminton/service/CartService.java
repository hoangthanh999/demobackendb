// backend/src/main/java/com/badminton/service/CartService.java
package com.badminton.service;

import com.badminton.dto.request.AddToCartRequest;
import com.badminton.dto.request.UpdateCartItemRequest;
import com.badminton.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(Long userId, Long cartItemId);

    void clearCart(Long userId);

    Integer getCartItemCount(Long userId);
}
