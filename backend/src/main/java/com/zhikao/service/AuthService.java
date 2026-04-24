package com.zhikao.service;

import com.zhikao.dto.AuthResponse;
import com.zhikao.dto.LoginRequest;
import com.zhikao.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
