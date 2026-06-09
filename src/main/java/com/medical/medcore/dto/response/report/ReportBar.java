package com.medical.medcore.dto.response.report;

/**
 * Barra de una sección "bars". {@code value} = magnitud para el ancho;
 * {@code display} = texto mostrado (ya formateado).
 */
public record ReportBar(String label, double value, String display) {
}
