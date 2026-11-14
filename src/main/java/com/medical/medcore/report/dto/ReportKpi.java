package com.medical.medcore.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
