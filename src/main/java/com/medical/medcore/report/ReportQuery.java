package com.medical.medcore.report;

import java.time.LocalDate;

/**
 * Filtros comunes de un reporte. Todos opcionales salvo lo que cada provider exija.
 * {@code from}/{@code to} se normalizan al rango efectivo (mes actual si faltan ambos)
 * en {@link ReportService}.
 */
public record ReportQuery(
        LocalDate from,
        LocalDate to,
        Long branchId,
        Long doctorId,
        Long specialtyId,
        String status
) {
    /** Inicio del rango como datetime inclusivo (00:00 del día {@code from}). */
    public java.time.LocalDateTime fromDateTime() {
        return from.atStartOfDay();
    }

    /** Fin del rango como datetime exclusivo (00:00 del día siguiente a {@code to}). */
    public java.time.LocalDateTime toDateTimeExclusive() {
        return to.plusDays(1).atStartOfDay();
    }
}
