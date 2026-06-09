package com.medical.medcore.dto.response.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Sección de lista etiqueta/valor. */
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
