package com.medical.medcore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorScheduleRequest(
        @NotNull Long doctorBranchId,
        @NotNull @Min(0) @Max(6) Integer dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Integer slotDurationMinutes,
        Integer maxPatientsPerSlot,
        LocalDate validFrom,
        LocalDate validUntil,
        Boolean isActive
) {}
