package com.medical.medcore.report.provider.clinic;

import com.medical.medcore.dto.response.report.BarsSection;
import com.medical.medcore.dto.response.report.ReportBar;
import com.medical.medcore.dto.response.report.ReportColumn;
import com.medical.medcore.dto.response.report.ReportKpi;
import com.medical.medcore.dto.response.report.ReportResult;
import com.medical.medcore.dto.response.report.ReportSection;
import com.medical.medcore.dto.response.report.TableSection;
import com.medical.medcore.report.ReportContext;
import com.medical.medcore.report.ReportProvider;
import com.medical.medcore.report.ReportQuery;
import com.medical.medcore.report.provider.BaseReportProvider;
import com.medical.medcore.report.support.BranchFilter;
import com.medical.medcore.report.support.ReportFormatter;
import com.medical.medcore.repository.report.ReportingPaymentRepository;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClinicFinancialSummaryProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingPaymentRepository payments;

    @Override
    public String key() {
        return "clinic-financial-summary";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.ADMIN, RoleConstants.SUPER_ADMIN);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        BranchFilter bf = resolveBranches(q, ctx);
        Long t = ctx.tenantId();

        BigDecimal total = payments.totalCollected(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        BigDecimal pending = payments.totalPending(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        long count = payments.countCollected(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        BigDecimal avgTicket = count > 0
                ? total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Ingresos del periodo", ReportFormatter.currency(total), "payments", "green"),
                ReportKpi.of("Cobros pendientes", ReportFormatter.currency(pending), "pending", "orange"),
                ReportKpi.of("Ticket promedio", ReportFormatter.currency(avgTicket), "receipt_long", "blue"),
                ReportKpi.of("Nº de pagos", ReportFormatter.number(count), "point_of_sale", "purple")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> branchBars = payments.byBranch(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())
                .stream()
                .map(r -> new ReportBar(asString(r[0]), asBig(r[1]).doubleValue(), ReportFormatter.currency(asBig(r[1]))))
                .toList();
        if (!branchBars.isEmpty()) {
            sections.add(new BarsSection("Ingresos por sucursal", branchBars));
        }

        List<Map<String, Object>> methodRows = new ArrayList<>();
        for (Object[] r : payments.byMethod(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("method", asString(r[0]));
            row.put("count", asLong(r[1]));
            row.put("total", asBig(r[2]));
            methodRows.add(row);
        }
        sections.add(new TableSection("Detalle por método de pago", List.of(
                ReportColumn.text("method", "Método"),
                ReportColumn.number("count", "Operaciones"),
                ReportColumn.currency("total", "Total")
        ), methodRows));

        List<Map<String, Object>> dayRows = new ArrayList<>();
        for (Object[] r : payments.byDay(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", asString(r[0]));
            row.put("total", asBig(r[2]));
            dayRows.add(row);
        }
        sections.add(new TableSection("Ingresos por día", List.of(
                ReportColumn.date("date", "Fecha"),
                ReportColumn.currency("total", "Total")
        ), dayRows));

        return new ReportResult(key(), "Resumen Financiero", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
