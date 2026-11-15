package com.medical.medcore.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivationTokenRequest(@NotBlank(message = "El código de activación es obligatorio") String token) {
}
