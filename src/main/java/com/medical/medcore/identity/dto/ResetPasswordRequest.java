package com.medical.medcore.identity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @JsonAlias("email")
        @NotBlank(message = "Ingresa tu correo o número de documento") @Size(max = 150) String identifier,
        @NotBlank(message = "Ingresa el código que te enviamos") @Size(max = 12) String code,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres") String newPassword
) {
}
