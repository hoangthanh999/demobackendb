package com.badminton.service;

import com.badminton.dto.request.LoginRequest;
import com.badminton.dto.request.RegisterRequest;
import com.badminton.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
