package com.medical.medcore.report.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
public class PdfReportExporter implements ReportExporter {

    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color HEADER_BG = new Color(243, 244, 246);
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
    private static final Font META = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font TD = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font KPI_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    private static final Font KPI_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);

    @Override
    public ReportFormat format() {
        return ReportFormat.PDF;
    }

    @Override
    public byte[] export(ReportResult result) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph(result.title(), TITLE));
            if (result.subtitle() != null) {
                document.add(new Paragraph(result.subtitle(), SUBTITLE));
            }
            if (result.range() != null) {
                document.add(new Paragraph(
                        "Periodo: " + result.range().from() + " a " + result.range().to(), META));
            }
            if (result.generatedAt() != null) {
                document.add(new Paragraph("Generado: " + result.generatedAt(), META));
            }
            document.add(spacer());

            if (result.kpis() != null && !result.kpis().isEmpty()) {
                document.add(kpiGrid(result.kpis()));
                document.add(spacer());
            }

            if (result.sections() != null) {
                for (ReportSection section : result.sections()) {
                    addSection(document, section);
                }
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el archivo PDF", e);
        }
    }

    private PdfPTable kpiGrid(List<ReportKpi> kpis) throws DocumentException {
        int cols = Math.min(4, Math.max(1, kpis.size()));
        PdfPTable grid = new PdfPTable(cols);
        grid.setWidthPercentage(100);
        for (ReportKpi kpi : kpis) {
            PdfPCell cell = new PdfPCell();
            cell.setPadding(8);
            cell.setBorderColor(new Color(229, 231, 235));
            cell.addElement(new Paragraph(kpi.label() != null ? kpi.label() : "", KPI_LABEL));
            cell.addElement(new Paragraph(kpi.value() != null ? kpi.value() : "", KPI_VALUE));
            if (kpi.trend() != null && kpi.trend().label() != null) {
                cell.addElement(new Paragraph(kpi.trend().label(), META));
            }
            grid.addCell(cell);
        }
        int remainder = kpis.size() % cols;
        if (remainder != 0) {
            for (int i = 0; i < cols - remainder; i++) {
                PdfPCell empty = new PdfPCell();
                empty.setBorder(0);
                grid.addCell(empty);
            }
        }
        return grid;
    }

    private void addSection(Document document, ReportSection section) throws DocumentException {
        document.add(new Paragraph(section.title(), SECTION));
        document.add(spacer(4));
        if (section instanceof TableSection table) {
            document.add(tableOf(table));
        } else if (section instanceof BarsSection bars) {
            document.add(barsOf(bars));
        } else if (section instanceof ListSection list) {
            document.add(listOf(list));
        }
        document.add(spacer());
    }

    private PdfPTable tableOf(TableSection table) throws DocumentException {
        List<ReportColumn> columns = table.columns();
        PdfPTable pdf = new PdfPTable(Math.max(1, columns.size()));
        pdf.setWidthPercentage(100);
        for (ReportColumn col : columns) {
            pdf.addCell(headerCell(col.label(), col.align()));
        }
        if (table.rows() != null) {
            for (Map<String, Object> row : table.rows()) {
                for (ReportColumn col : columns) {
                    pdf.addCell(bodyCell(stringify(row.get(col.key())), col.align()));
                }
            }
        }
        return pdf;
    }

    private PdfPTable barsOf(BarsSection bars) throws DocumentException {
        PdfPTable pdf = new PdfPTable(2);
        pdf.setWidthPercentage(100);
        pdf.setWidths(new int[]{3, 1});
        if (bars.bars() != null) {
            for (ReportBar bar : bars.bars()) {
                pdf.addCell(bodyCell(bar.label(), "left"));
                pdf.addCell(bodyCell(bar.display(), "right"));
            }
        }
        return pdf;
    }

    private PdfPTable listOf(ListSection list) {
        PdfPTable pdf = new PdfPTable(2);
        pdf.setWidthPercentage(100);
        if (list.items() != null) {
            for (ReportListItem item : list.items()) {
                String label = item.hint() != null ? item.label() + " — " + item.hint() : item.label();
                pdf.addCell(bodyCell(label, "left"));
                pdf.addCell(bodyCell(item.value(), "right"));
            }
        }
        return pdf;
    }

    private PdfPCell headerCell(String text, String align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", TH));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment(align));
        return cell;
    }

    private PdfPCell bodyCell(String text, String align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", TD));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment(align));
        return cell;
    }

    private int alignment(String align) {
        if ("right".equals(align)) {
            return Element.ALIGN_RIGHT;
        }
        if ("center".equals(align)) {
            return Element.ALIGN_CENTER;
        }
        return Element.ALIGN_LEFT;
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Paragraph spacer() {
        return spacer(10);
    }

    private Paragraph spacer(float leading) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(leading);
        return p;
    }
}
