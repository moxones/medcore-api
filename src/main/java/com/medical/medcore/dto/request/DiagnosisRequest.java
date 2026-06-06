package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DiagnosisRequest(
        Long cie10Id,
        @NotBlank(message = "La descripción del diagnóstico es obligatoria") String description,
        String diagnosisType,
        String diagnosisRank,
        String notes
) {}
