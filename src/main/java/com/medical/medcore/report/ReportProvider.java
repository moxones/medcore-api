package com.medical.medcore.report;

import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.report.dto.ReportResult;

import java.util.Set;

public interface ReportProvider {

    String key();

    Set<String> allowedRoles();

    default Set<ClinicModule> requiredModules() {
        return Set.of();
    }

    ReportResult generate(ReportQuery query, ReportContext ctx);
}
