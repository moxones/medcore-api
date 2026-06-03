package com.medical.medcore.dto.response;

public record SpecialtySummaryResponse(
        Long id,
        String code,
        String name,
        int doctorCount,
        String nextAvailableDate
) {}
