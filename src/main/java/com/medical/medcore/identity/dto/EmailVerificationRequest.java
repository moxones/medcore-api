package com.medical.medcore.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationRequest(
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email inválido") @Size(max = 150) String email
) {
}
