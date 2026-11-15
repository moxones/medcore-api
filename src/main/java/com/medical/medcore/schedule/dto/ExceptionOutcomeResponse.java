package com.medical.medcore.schedule.dto;

public record ExceptionOutcomeResponse(
        ScheduleExceptionResponse exception,
        int flagged,
        int cancelled,
        int restored
) {
}
