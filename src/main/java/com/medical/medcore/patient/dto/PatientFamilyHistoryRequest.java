package com.medical.medcore.patient.dto;

import jakarta.validation.constraints.NotBlank;

public record PatientFamilyHistoryRequest(
        @NotBlank(message = "El parentesco es obligatorio") String relationship,
        @NotBlank(message = "La condición es obligatoria") String condition,
        String notes
) {}
