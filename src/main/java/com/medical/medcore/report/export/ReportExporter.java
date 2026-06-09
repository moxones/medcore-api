package com.medical.medcore.report.export;

import com.medical.medcore.dto.response.report.ReportResult;
import com.medical.medcore.report.ReportFormat;

/** Convierte un {@link ReportResult} genérico a un archivo binario para descarga. */
public interface ReportExporter {

    ReportFormat format();

    byte[] export(ReportResult result);
}
