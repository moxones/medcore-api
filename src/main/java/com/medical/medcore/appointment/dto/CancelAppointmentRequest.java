package com.medical.medcore.appointment.dto;

import com.medical.medcore.appointment.domain.CancellationOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
        @NotBlank(message = "El motivo de cancelación es obligatorio") @Size(max = 500) String reason,
        CancellationOrigin origin
) {
}
