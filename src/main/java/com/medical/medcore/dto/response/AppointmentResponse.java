package com.medical.medcore.dto.response;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        String patientPhone,
        Long doctorId,
        String doctorName,
        Long branchId,
        String branchName,
        LocalDateTime scheduledAt,
        Long statusId,
        Long appointmentTypeId,
        String reason,
        Integer durationMinutes,
        String flowStatus,
        LocalDateTime createdAt,
        String bookingSource
) {}
