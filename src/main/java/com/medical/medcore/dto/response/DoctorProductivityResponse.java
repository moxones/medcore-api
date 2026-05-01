package com.medical.medcore.dto.response;

public record DoctorProductivityResponse(
        Long doctorId,
        String doctorName,
        long totalAppointments
) {}
