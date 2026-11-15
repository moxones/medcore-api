package com.medical.medcore.appointment.dto;

public record RescheduleSuggestionResponse(
        String date,
        String startTime,
        String endTime,
        Long doctorId,
        String doctorName,
        boolean sameWeek,
        boolean sameTime
) {
}
