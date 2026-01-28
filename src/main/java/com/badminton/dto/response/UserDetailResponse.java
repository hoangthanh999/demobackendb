package com.badminton.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String address;
    private String avatar;
    private Boolean active;
    private String lastLogin;
    private Integer totalBookings;
    private Integer totalOrders;
    private Double totalSpent;
    private String createdAt;
}
