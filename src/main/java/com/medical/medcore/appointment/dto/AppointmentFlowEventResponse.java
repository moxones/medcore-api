package com.medical.medcore.appointment.dto;

import java.time.LocalDateTime;

public record AppointmentFlowEventResponse(
        Long id,
        String fromStatus,
        String toStatus,
        LocalDateTime changedAt,
        Long changedBy,
        String note
) {
}
