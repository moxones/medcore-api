package com.medical.medcore.dto.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Sección de barras horizontales. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BarsSection(
        String title,
        String unit,
        List<ReportBar> bars
) implements ReportSection {

    public BarsSection(String title, List<ReportBar> bars) {
        this(title, null, bars);
    }

    @Override
    @JsonProperty("type")
    public String type() {
        return "bars";
    }
}
