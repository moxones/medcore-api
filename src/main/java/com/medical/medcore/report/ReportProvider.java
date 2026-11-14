package com.medical.medcore.report;

import com.medical.medcore.report.dto.ReportResult;

import java.util.Set;

public interface ReportProvider {

    String key();

    Set<String> allowedRoles();

    ReportResult generate(ReportQuery query, ReportContext ctx);
}
