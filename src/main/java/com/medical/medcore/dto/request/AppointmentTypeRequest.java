package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AppointmentTypeRequest(
        @NotBlank(message = "El código es obligatorio") String code,
        @NotBlank(message = "El nombre es obligatorio") String name,
        @Positive(message = "La duración debe ser mayor a 0") Integer durationMinutes,
        Boolean isActive
) {}
