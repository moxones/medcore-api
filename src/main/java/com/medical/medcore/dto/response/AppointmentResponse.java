package com.medical.medcore.dto.response;

import java.math.BigDecimal;
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
        String status,
        Long appointmentTypeId,
        String reason,
        Integer durationMinutes,
        String flowStatus,
        LocalDateTime createdAt,
        String bookingSource,
        // Timestamps por transición del flujo (ISO 8601, nullables).
        LocalDateTime checkedInAt,
        LocalDateTime calledAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime completedAt,
        // Monto a cobrar para la columna "Por cobrar". Null hasta que exista
        // una fuente de precio por tipo de cita.
        BigDecimal amount
) {}
