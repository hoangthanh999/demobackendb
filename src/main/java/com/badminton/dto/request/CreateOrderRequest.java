// backend/src/main/java/com/badminton/dto/request/CreateOrderRequest.java
package com.badminton.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Danh sách sản phẩm không được để trống")
    private List<OrderItemRequest> items;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String shippingProvince;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String shippingDistrict;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String shippingWard;

    private String note;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    private String couponCode; // Mã giảm giá (optional)

    public enum PaymentMethod {
        COD, MOMO, BANK_TRANSFER, ONLINE
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "ID sản phẩm không được để trống")
        private Long productId;

        @NotNull(message = "Số lượng không được để trống")
        private Integer quantity;
    }
}
