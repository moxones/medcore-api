package com.medical.medcore.dto.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Columna de una tabla. {@code align}: left|center|right.
 * {@code format}: text|number|currency|percent|date (lo aplica el front en tablas).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportColumn(String key, String label, String align, String format) {

    public static ReportColumn text(String key, String label) {
        return new ReportColumn(key, label, "left", "text");
    }

    public static ReportColumn number(String key, String label) {
        return new ReportColumn(key, label, "right", "number");
    }

    public static ReportColumn currency(String key, String label) {
        return new ReportColumn(key, label, "right", "currency");
    }

    public static ReportColumn percent(String key, String label) {
        return new ReportColumn(key, label, "right", "percent");
    }

    public static ReportColumn date(String key, String label) {
        return new ReportColumn(key, label, "left", "date");
    }
}
