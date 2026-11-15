package com.medical.medcore.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationProfileRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
        @Size(max = 150) String subtitle,
        @Email(message = "Email de contacto inválido") @Size(max = 150) String contactEmail,
        @Size(max = 50) String contactPhone,
        @Size(max = 255) String address,
        @Size(max = 20) String taxId
) {
}
