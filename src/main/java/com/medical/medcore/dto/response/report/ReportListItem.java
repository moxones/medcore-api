package com.medical.medcore.dto.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Ítem de una sección "list". {@code value} ya formateado. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportListItem(String label, String value, String hint) {

    public static ReportListItem of(String label, String value) {
        return new ReportListItem(label, value, null);
    }
}
