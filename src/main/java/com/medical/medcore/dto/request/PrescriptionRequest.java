package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PrescriptionRequest(
        @NotBlank(message = "El medicamento es obligatorio") String medication,
        String dosage,
        String frequency,
        String duration,
        String route,
        String quantity,
        String presentation,
        String instructions
) {}
