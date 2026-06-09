package com.medical.medcore.dto.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Tarjeta KPI superior. {@code value} viene YA FORMATEADO como string
 * ("S/ 12,450", "87%", "1,204"); el front no recalcula.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportKpi(
        String label,
        String value,
        String icon,
        String hint,
        String variant,
        ReportTrend trend
) {
    public static ReportKpi of(String label, String value, String icon, String variant) {
        return new ReportKpi(label, value, icon, null, variant, null);
    }
}
