package com.medical.medcore.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorScheduleResponse(
        Long id,
        Long doctorId,
        Long doctorBranchId,
        Long branchId,
        String branchName,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Boolean isActive,
        Integer slotDurationMinutes,
        Integer maxPatientsPerSlot,
        LocalDate validFrom,
        LocalDate validUntil
) {}
