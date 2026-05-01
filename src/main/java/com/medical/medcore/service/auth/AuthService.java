package com.medical.medcore.service.auth;

import com.medical.medcore.dto.auth.RegisterRequest;
import com.medical.medcore.dto.request.LoginRequest;
import com.medical.medcore.dto.response.AuthResponse;
import com.medical.medcore.dto.response.UserMeResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken);

    AuthResponse register(RegisterRequest request);

    UserMeResponse me();
}