package com.medical.medcore.report.export;

import com.medical.medcore.dto.response.report.BarsSection;
import com.medical.medcore.dto.response.report.ListSection;
import com.medical.medcore.dto.response.report.ReportBar;
import com.medical.medcore.dto.response.report.ReportColumn;
import com.medical.medcore.dto.response.report.ReportKpi;
import com.medical.medcore.dto.response.report.ReportListItem;
import com.medical.medcore.dto.response.report.ReportResult;
import com.medical.medcore.dto.response.report.ReportSection;
import com.medical.medcore.dto.response.report.TableSection;
import com.medical.medcore.report.ReportFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class XlsxReportExporter implements ReportExporter {

    @Override
    public ReportFormat format() {
        return ReportFormat.XLSX;
    }

    @Override
    public byte[] export(ReportResult result) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle header = headerStyle(workbook);
            Set<String> usedNames = new HashSet<>();

            writeSummarySheet(workbook, result, header, usedNames);
            if (result.sections() != null) {
                for (ReportSection section : result.sections()) {
                    writeSection(workbook, section, header, usedNames);
                }
            }

            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo XLSX", e);
        }
    }

    private void writeSummarySheet(Workbook wb, ReportResult result, CellStyle header, Set<String> used) {
        Sheet sheet = wb.createSheet(uniqueName("Resumen", used));
        int r = 0;

        Row titleRow = sheet.createRow(r++);
        cell(titleRow, 0, result.title(), header);
        if (result.subtitle() != null) {
            cell(sheet.createRow(r++), 0, result.subtitle(), null);
        }
        if (result.range() != null) {
            cell(sheet.createRow(r++), 0,
                    "Periodo: " + result.range().from() + " a " + result.range().to(), null);
        }
        if (result.generatedAt() != null) {
            cell(sheet.createRow(r++), 0, "Generado: " + result.generatedAt(), null);
        }
        r++;

        if (result.kpis() != null && !result.kpis().isEmpty()) {
            Row kpiHeader = sheet.createRow(r++);
            cell(kpiHeader, 0, "Indicador", header);
            cell(kpiHeader, 1, "Valor", header);
            for (ReportKpi kpi : result.kpis()) {
                Row row = sheet.createRow(r++);
                cell(row, 0, kpi.label(), null);
                cell(row, 1, kpi.value(), null);
            }
        }
        autosize(sheet, 2);
    }

    private void writeSection(Workbook wb, ReportSection section, CellStyle header, Set<String> used) {
        if (section instanceof TableSection table) {
            writeTable(wb, table, header, used);
        } else if (section instanceof BarsSection bars) {
            writeBars(wb, bars, header, used);
        } else if (section instanceof ListSection list) {
            writeList(wb, list, header, used);
        }
    }

    private void writeTable(Workbook wb, TableSection table, CellStyle header, Set<String> used) {
        Sheet sheet = wb.createSheet(uniqueName(table.title(), used));
        List<ReportColumn> columns = table.columns();
        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < columns.size(); c++) {
            cell(headerRow, c, columns.get(c).label(), header);
        }
        int r = 1;
        if (table.rows() != null) {
            for (Map<String, Object> row : table.rows()) {
                Row dataRow = sheet.createRow(r++);
                for (int c = 0; c < columns.size(); c++) {
                    writeValue(dataRow, c, row.get(columns.get(c).key()));
                }
            }
        }
        autosize(sheet, columns.size());
    }

    private void writeBars(Workbook wb, BarsSection bars, CellStyle header, Set<String> used) {
        Sheet sheet = wb.createSheet(uniqueName(bars.title(), used));
        Row headerRow = sheet.createRow(0);
        cell(headerRow, 0, "Etiqueta", header);
        cell(headerRow, 1, "Valor", header);
        cell(headerRow, 2, "Mostrado", header);
        int r = 1;
        if (bars.bars() != null) {
            for (ReportBar bar : bars.bars()) {
                Row row = sheet.createRow(r++);
                cell(row, 0, bar.label(), null);
                Cell v = row.createCell(1);
                v.setCellValue(bar.value());
                cell(row, 2, bar.display(), null);
            }
        }
        autosize(sheet, 3);
    }

    private void writeList(Workbook wb, ListSection list, CellStyle header, Set<String> used) {
        Sheet sheet = wb.createSheet(uniqueName(list.title(), used));
        Row headerRow = sheet.createRow(0);
        cell(headerRow, 0, "Etiqueta", header);
        cell(headerRow, 1, "Valor", header);
        cell(headerRow, 2, "Detalle", header);
        int r = 1;
        if (list.items() != null) {
            for (ReportListItem item : list.items()) {
                Row row = sheet.createRow(r++);
                cell(row, 0, item.label(), null);
                cell(row, 1, item.value(), null);
                cell(row, 2, item.hint(), null);
            }
        }
        autosize(sheet, 3);
    }

    private void writeValue(Row row, int col, Object value) {
        Cell cell = row.createCell(col);
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void autosize(Sheet sheet, int columns) {
        for (int c = 0; c < columns; c++) {
            sheet.setColumnWidth(c, 22 * 256);
        }
    }

    private String uniqueName(String raw, Set<String> used) {
        String base = WorkbookUtil.createSafeSheetName(raw == null || raw.isBlank() ? "Hoja" : raw);
        String name = base;
        int i = 2;
        while (!used.add(name.toLowerCase())) {
            String suffix = " (" + i++ + ")";
            String trimmed = base.length() + suffix.length() > 31
                    ? base.substring(0, 31 - suffix.length())
                    : base;
            name = trimmed + suffix;
        }
        return name;
    }
}
