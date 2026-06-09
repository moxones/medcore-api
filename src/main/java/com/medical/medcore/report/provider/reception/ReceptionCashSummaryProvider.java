package com.medical.medcore.report.provider.reception;

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
public class ReceptionCashSummaryProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingPaymentRepository payments;

    @Override
    public String key() {
        return "reception-cash-summary";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.RECEPTIONIST);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        BranchFilter bf = resolveBranches(q, ctx);
        Long t = ctx.tenantId();

        BigDecimal total = payments.totalCollected(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        long count = payments.countCollected(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        BigDecimal pending = payments.totalPending(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());

        List<Object[]> byMethod = payments.byMethod(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        String topMethod = byMethod.isEmpty() ? "—" : asString(byMethod.get(0)[0]);

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Total cobrado", ReportFormatter.currency(total), "payments", "green"),
                ReportKpi.of("Nº de operaciones", ReportFormatter.number(count), "receipt_long", "blue"),
                new ReportKpi("Método más usado", topMethod, "credit_card", null, "purple", null),
                ReportKpi.of("Pendiente de cobro", ReportFormatter.currency(pending), "pending", "orange")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> methodBars = byMethod.stream()
                .map(r -> new ReportBar(asString(r[0]), asBig(r[2]).doubleValue(), ReportFormatter.currency(asBig(r[2]))))
                .toList();
        if (!methodBars.isEmpty()) {
            sections.add(new BarsSection("Cobros por método", methodBars));
        }

        List<Map<String, Object>> dayRows = new ArrayList<>();
        for (Object[] r : payments.byDay(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", asString(r[0]));
            row.put("operations", asLong(r[1]));
            row.put("total", asBig(r[2]));
            dayRows.add(row);
        }
        sections.add(new TableSection("Cobros por día", List.of(
                ReportColumn.date("date", "Fecha"),
                ReportColumn.number("operations", "Operaciones"),
                ReportColumn.currency("total", "Total")
        ), dayRows));

        return new ReportResult(key(), "Resumen de Cobros", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
