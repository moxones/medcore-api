package com.medical.medcore.dto.response;

import java.util.Map;

public record DoctorBranchScheduleResponse(
        Long branchId,
        String branchName,
        Map<String, String> scheduleByDay
) {}