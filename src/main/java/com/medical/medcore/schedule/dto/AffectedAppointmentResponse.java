package com.medical.medcore.schedule.dto;

import java.time.LocalDateTime;

public record AffectedAppointmentResponse(
        Long id,
        LocalDateTime scheduledAt,
        Integer durationMinutes,
        Long patientId,
        String patientName,
        String patientPhone,
        Long doctorId,
        String doctorName,
        Long branchId,
        String branchName,
        String flowStatus,
        boolean alreadyFlagged
) {
}
