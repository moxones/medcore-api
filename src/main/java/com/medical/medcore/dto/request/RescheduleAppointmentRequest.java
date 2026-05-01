package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(
        @NotNull @Future LocalDateTime newScheduledAt,
        String reason
) {}
