package com.medical.medcore.dto.response.report;

/** Tendencia opcional de un KPI, p. ej. "+12% vs periodo anterior". */
public record ReportTrend(String direction, String label) {

    public static ReportTrend up(String label) {
        return new ReportTrend("up", label);
    }

    public static ReportTrend down(String label) {
        return new ReportTrend("down", label);
    }

    public static ReportTrend flat(String label) {
        return new ReportTrend("flat", label);
    }
}
