package com.medical.medcore.schedule.dto;

import java.time.LocalDate;

public record BranchClosureResponse(
        Long id,
        Long branchId,
        String branchName,
        LocalDate date,
        String name,
        String source,
        long pendingReschedule
) {
}
