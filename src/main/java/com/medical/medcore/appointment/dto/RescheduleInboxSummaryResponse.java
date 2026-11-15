package com.medical.medcore.appointment.dto;

public record RescheduleInboxSummaryResponse(
        long total,
        long today,
        long overdue
) {
}
