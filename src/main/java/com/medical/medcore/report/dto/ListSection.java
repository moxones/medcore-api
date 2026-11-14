package com.medical.medcore.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ListSection(
        String title,
        List<ReportListItem> items
) implements ReportSection {

    @Override
    @JsonProperty("type")
    public String type() {
        return "list";
    }
}
