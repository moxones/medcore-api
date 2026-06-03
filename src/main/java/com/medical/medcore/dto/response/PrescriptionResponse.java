package com.medical.medcore.dto.response;

public record PrescriptionResponse(
        Long id,
        String medication,
        String dosage,
        String frequency,
        String duration,
        String instructions
) {}
