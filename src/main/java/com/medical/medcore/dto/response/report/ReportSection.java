package com.medical.medcore.dto.response.report;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TableSection.class, name = "table"),
        @JsonSubTypes.Type(value = BarsSection.class, name = "bars"),
        @JsonSubTypes.Type(value = ListSection.class, name = "list")
})
public sealed interface ReportSection permits TableSection, BarsSection, ListSection {

    String type();

    String title();
}
