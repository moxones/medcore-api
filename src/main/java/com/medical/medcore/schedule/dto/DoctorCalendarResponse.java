package com.medical.medcore.schedule.dto;

import java.util.List;

public record DoctorCalendarResponse(
        Long doctorId,
        String doctorName,
        List<CalendarBranchResponse> branches,
        List<CalendarDayResponse> days,
        List<ScheduleExceptionResponse> exceptions
) {
}
