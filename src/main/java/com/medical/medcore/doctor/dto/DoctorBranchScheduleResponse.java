package com.medical.medcore.doctor.dto;

import java.util.Map;

public record DoctorBranchScheduleResponse(
        Long branchId,
        String branchName,
        Map<String, String> scheduleByDay
) {}