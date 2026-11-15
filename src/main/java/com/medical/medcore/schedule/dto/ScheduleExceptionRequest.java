package com.medical.medcore.schedule.dto;

import com.medical.medcore.schedule.domain.AffectedResolution;
import com.medical.medcore.schedule.domain.ExceptionKind;
import com.medical.medcore.schedule.domain.ExceptionReason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleExceptionRequest(
        @NotNull(message = "El profesional es obligatorio") Long doctorId,
        Long branchId,
        @NotNull(message = "Indica si es una ausencia o horas extra") ExceptionKind kind,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        @Min(value = 5, message = "La duración mínima por turno es 5 minutos")
        @Max(value = 480, message = "La duración máxima por turno es 480 minutos") Integer slotDurationMinutes,
        ExceptionReason reason,
        @Size(max = 255) String note,
        AffectedResolution resolution,
        @Size(max = 255) String cancelMessage
) {
}
