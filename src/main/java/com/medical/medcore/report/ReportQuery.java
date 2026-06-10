package com.medical.medcore.report;

import java.time.LocalDate;

public record ReportQuery(
        LocalDate from,
        LocalDate to,
        Long branchId,
        Long doctorId,
        Long specialtyId,
        String status
) {
    public java.time.LocalDateTime fromDateTime() {
        return from.atStartOfDay();
    }

    public java.time.LocalDateTime toDateTimeExclusive() {
        return to.plusDays(1).atStartOfDay();
    }
}
