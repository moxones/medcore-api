package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PatientHabitRequest(
        @NotBlank(message = "El tipo de hábito es obligatorio") String habitType,
        String detail,
        String status
) {}
