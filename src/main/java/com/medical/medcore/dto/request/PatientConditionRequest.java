package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PatientConditionRequest(
        Long cie10Id,
        @NotBlank(message = "La descripción es obligatoria") String description,
        String status,
        LocalDate diagnosedAt
) {}
