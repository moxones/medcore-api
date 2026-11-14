package com.medical.medcore.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

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
