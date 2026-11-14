package com.medical.medcore.appointment.dto;

public record SpecialtySummaryResponse(
        Long id,
        String code,
        String name,
        int doctorCount,
        String nextAvailableDate
) {}
