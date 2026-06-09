package com.medical.medcore.dto.response.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Sección de tabla. Cada row mapea {@code column.key -> valor}. */
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
