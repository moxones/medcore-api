package com.medical.medcore.schedule.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ScheduleExceptionResponse(
        Long id,
        Long doctorId,
        String doctorName,
        Long branchId,
        String branchName,
        String kind,
        LocalDate startDate,
        LocalDate endDate,
        String startTime,
        String endTime,
        boolean fullDay,
        Integer slotDurationMinutes,
        String reason,
        String reasonLabel,
        String note,
        String status,
        LocalDateTime createdAt,
        String createdByName,
        LocalDateTime reviewedAt,
        String reviewNote,
        LocalDateTime cancelledAt,
        long affectedAppointments,
        long pendingReschedule
) {
}
