package com.medical.medcore.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record TenantAppointmentTypeRequest(
        @Min(value = 5, message = "La duración mínima es de 5 minutos")
        @Max(value = 480, message = "La duración máxima es de 480 minutos") Integer durationMinutes,
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 2 decimales") BigDecimal price
) {
}
