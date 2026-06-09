package com.medical.medcore.dto.response.report;

/** Rango de fechas del reporte (strings ya legibles o ISO yyyy-MM-dd). */
public record ReportRange(String from, String to) {
}
