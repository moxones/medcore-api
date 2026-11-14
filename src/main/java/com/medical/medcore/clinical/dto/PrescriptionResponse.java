package com.medical.medcore.clinical.dto;

public record PrescriptionResponse(
        Long id,
        String medication,
        String dosage,
        String frequency,
        String duration,
        String route,
        String quantity,
        String presentation,
        Boolean isActive,
        String instructions
) {}
