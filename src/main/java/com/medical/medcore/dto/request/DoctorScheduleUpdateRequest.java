package com.medical.medcore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorScheduleUpdateRequest(
        Long doctorBranchId,
        @Min(0) @Max(6) Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        Integer maxPatientsPerSlot,
        LocalDate validFrom,
        LocalDate validUntil,
        Boolean isActive
) {}
