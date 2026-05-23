package com.medical.medcore.dto.request;

import com.medical.medcore.entity.enums.BookingSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

public record CreateAppointmentRequest(
        @NotNull Long patientId,
        @NotNull Long doctorId,
        @NotNull Long branchId,
        @NotNull @Future LocalDateTime scheduledAt,
        Long appointmentTypeId,
        String reason,
        BookingSource bookingSource
) {}
