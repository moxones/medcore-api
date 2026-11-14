package com.medical.medcore.report.export;

import com.medical.medcore.report.dto.ReportResult;
import com.medical.medcore.report.ReportFormat;

public interface ReportExporter {

    ReportFormat format();

    byte[] export(ReportResult result);
}
