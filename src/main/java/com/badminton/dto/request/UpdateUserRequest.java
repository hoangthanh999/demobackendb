package com.badminton.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;

    private String role; // USER, OWNER, ADMIN

    private String address;

    private String avatar;

    private Boolean active;
}
