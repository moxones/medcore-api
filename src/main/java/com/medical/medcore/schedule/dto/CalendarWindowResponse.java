package com.medical.medcore.schedule.dto;

public record CalendarWindowResponse(
        Long branchId,
        String branchName,
        String startTime,
        String endTime,
        String source
) {
}
