package com.medical.medcore.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HolidayImportRequest(
        @NotNull(message = "El año es obligatorio")
        @Min(value = 2024, message = "Año inválido") @Max(value = 2100, message = "Año inválido") Integer year
) {
}
