package com.medical.medcore.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank(message = "El código es obligatorio") @Size(max = 50) String code,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String name,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 2 decimales") BigDecimal price,
        @Min(value = 1, message = "El máximo de usuarios debe ser al menos 1") Integer maxUsers,
        @Min(value = 1, message = "El máximo de sucursales debe ser al menos 1") Integer maxBranches,
        Boolean isActive
) {
}
