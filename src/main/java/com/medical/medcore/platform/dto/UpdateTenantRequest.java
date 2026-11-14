package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.TenantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
        @Size(max = 255) String logoUrl,
        @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "El color debe tener formato #RRGGBB") String primaryColor,
        @Size(max = 150) String subtitle,
        @Email(message = "Email de contacto inválido") @Size(max = 150) String contactEmail,
        @Size(max = 50) String contactPhone,
        @Size(max = 255) String address,
        @Size(max = 20) String taxId,
        @Size(max = 60) String timezone,
        @Pattern(regexp = "^$|^[A-Z]{3}$", message = "La moneda debe ser un código ISO de 3 letras") String currency,
        TenantStatus status
) {
}
