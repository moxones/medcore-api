package com.medical.medcore.appointment.dto;

import com.medical.medcore.appointment.domain.BookingSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateAppointmentRequest(
        Long patientId,
        @NotNull(message = "El médico es obligatorio") Long doctorId,
        @NotNull(message = "La sucursal es obligatoria") Long branchId,
        @NotNull(message = "La fecha y hora son obligatorias") LocalDateTime scheduledAt,
        Long appointmentTypeId,
        @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres") String reason,
        BookingSource bookingSource
) {
}
