package com.medical.medcore.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportListItem(String label, String value, String hint) {

    public static ReportListItem of(String label, String value) {
        return new ReportListItem(label, value, null);
    }
}
