package com.badminton.service;

import com.badminton.dto.request.UpdateProfileRequest;
import com.badminton.dto.response.UserResponse;
import com.badminton.entity.User;

import java.util.List;

public interface UserService {
    UserResponse getUserById(Long id);

    UserResponse updateProfile(Long id, UpdateProfileRequest request);

    List<UserResponse> getAllUsers();

    void deleteUser(Long id);

    User findByEmail(String email);

    User findByPhone(String phone);
}
