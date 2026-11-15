package com.medical.medcore.schedule.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarDayResponse(
        LocalDate date,
        List<CalendarWindowResponse> windows,
        long templateMinutes,
        long effectiveMinutes,
        List<String> closures,
        List<Long> exceptionIds,
        int appointments,
        int pendingReschedule
) {
}
