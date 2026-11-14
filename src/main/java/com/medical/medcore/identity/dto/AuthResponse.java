package com.medical.medcore.identity.dto;

public record AuthResponse(String accessToken, String refreshToken, boolean mustChangePassword) {
}
