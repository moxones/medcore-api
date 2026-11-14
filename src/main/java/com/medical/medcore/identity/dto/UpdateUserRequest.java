package com.medical.medcore.identity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateUserRequest(
        @Email(message = "Email inválido") @Size(max = 150) String email,
        @Valid PersonRequest person,
        List<Long> roleIds,
        List<String> roles
) {
}
