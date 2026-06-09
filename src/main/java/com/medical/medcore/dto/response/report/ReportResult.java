package com.medical.medcore.dto.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Sobre estándar de un reporte. El frontend tiene un único visor genérico que pinta
 * cualquier reporte a partir de esta estructura, por lo que los nombres de campo deben
 * respetarse exactamente (ver BACKEND_REPORTS_APIS.md §4).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportResult(
        String key,
        String title,
        String subtitle,
        String generatedAt,
        ReportRange range,
        List<ReportKpi> kpis,
        List<ReportSection> sections
) {
}
