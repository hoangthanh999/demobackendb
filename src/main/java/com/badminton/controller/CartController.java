// backend/src/main/java/com/badminton/controller/CartController.java
package com.badminton.controller;

import com.badminton.dto.request.AddToCartRequest;
import com.badminton.dto.request.UpdateCartItemRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.CartResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        CartResponse cart = cartService.getCart(user.getId());
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        CartResponse cart = cartService.addToCart(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Đã thêm vào giỏ hàng"));
    }

    @PutMapping("/items/{cartItemId}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        CartResponse cart = cartService.updateCartItem(user.getId(), cartItemId, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cập nhật giỏ hàng thành công"));
    }

    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        CartResponse cart = cartService.removeCartItem(user.getId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Đã xóa khỏi giỏ hàng"));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        cartService.clearCart(user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa toàn bộ giỏ hàng"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Integer count = cartService.getCartItemCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
