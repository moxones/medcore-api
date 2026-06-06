package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PatientAllergyRequest(
        @NotBlank(message = "El alérgeno es obligatorio") String allergen,
        String reaction,
        String severity,
        Boolean isActive
) {}
