package com.medical.medcore.identity.dto;

public record AuthResponse(String accessToken, String refreshToken, boolean mustChangePassword) {

    public AuthResponse withoutRefreshToken() {
        return new AuthResponse(accessToken, null, mustChangePassword);
    }
}
