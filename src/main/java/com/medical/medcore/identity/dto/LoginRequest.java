package com.medical.medcore.identity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @JsonAlias("email")
        @NotBlank(message = "Ingresa tu correo o número de documento") @Size(max = 150) String identifier,
        @NotBlank(message = "La contraseña es obligatoria") String password
) {
}
