package com.medical.medcore.service.auth;

import com.medical.medcore.dto.request.LoginRequest;
import com.medical.medcore.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken);
}