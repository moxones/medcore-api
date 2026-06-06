package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SaveNoteTemplateRequest(
        @NotBlank(message = "El nombre de la plantilla es obligatorio") String name,
        String chiefComplaint,
        String presentIllness,
        String physicalExamination,
        String assessment,
        String plan,
        String treatment,
        String notes
) {}
