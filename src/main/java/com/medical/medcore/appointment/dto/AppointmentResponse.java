package com.medical.medcore.appointment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        String patientPhone,
        Integer patientAge,
        String patientGender,
        String patientNumber,
        Long doctorId,
        String doctorName,
        Long branchId,
        String branchName,
        LocalDateTime scheduledAt,
        Long statusId,
        String status,
        Long appointmentTypeId,
        String appointmentTypeName,
        String reason,
        Integer durationMinutes,
        String flowStatus,
        String careStage,
        Boolean triageCompleted,
        LocalDateTime createdAt,
        String bookingSource,
        LocalDateTime checkedInAt,
        LocalDateTime calledAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime completedAt,
        BigDecimal amount,
        String cancelReason,
        LocalDateTime cancelledAt,
        LocalDateTime noShowAt
) {
}
