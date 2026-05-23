package com.medical.medcore.dto.response;

import java.util.List;
import java.util.Map;

public record DoctorCardResponse(
        Long id,
        Long personId,
        String fullName,
        String initials,
        String licenseNumber,
        Boolean isActive,
        Integer seniorityYears,
        List<String> specialties,
        List<String> branches,
        Boolean availableToday,
        Map<String, DoctorBranchScheduleResponse> weekScheduleByBranch,
        Long appointmentsThisMonth,
        Integer appointmentsGrowthPercent
) {}