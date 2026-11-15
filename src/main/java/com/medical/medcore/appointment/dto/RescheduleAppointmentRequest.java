package com.medical.medcore.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(
        @NotNull(message = "La nueva fecha y hora son obligatorias") LocalDateTime newScheduledAt,
        @Size(max = 500) String reason,
        Long doctorId
) {
}
