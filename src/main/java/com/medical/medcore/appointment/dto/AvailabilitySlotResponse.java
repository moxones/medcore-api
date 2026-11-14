package com.medical.medcore.appointment.dto;

public record AvailabilitySlotResponse(
        String startTime,
        String endTime,
        Long doctorId,
        String doctorName,
        String doctorInitials,
        String specialtyName,
        int durationMinutes
) {}
