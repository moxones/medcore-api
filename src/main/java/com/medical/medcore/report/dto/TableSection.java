package com.medical.medcore.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record TableSection(
        String title,
        List<ReportColumn> columns,
        List<Map<String, Object>> rows
) implements ReportSection {

    @Override
    @JsonProperty("type")
    public String type() {
        return "table";
    }
}
