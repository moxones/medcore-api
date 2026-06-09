package com.medical.medcore.report.provider.platform;

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
import com.medical.medcore.report.support.ReportFormatter;
import com.medical.medcore.repository.report.ReportingPlatformRepository;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ingresos por suscripciones. El monto usa {@code plans.price} como proxy (no hay historial de
 * facturación real); ARPA y MRR son estimaciones a partir de las suscripciones vigentes.
 */
@Component
@RequiredArgsConstructor
public class PlatformSubscriptionRevenueProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingPlatformRepository platform;

    @Override
    public String key() {
        return "platform-subscription-revenue";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.SUPER_ADMIN);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        List<Object[]> byPlan = platform.revenueByPlan();
        BigDecimal mrr = BigDecimal.ZERO;
        long accounts = 0;
        for (Object[] r : byPlan) {
            mrr = mrr.add(asBig(r[2]));
            accounts += asLong(r[1]);
        }
        BigDecimal arpa = accounts > 0
                ? mrr.divide(BigDecimal.valueOf(accounts), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Object[]> billed = platform.billedSubscriptions(q.from(), q.to());
        BigDecimal periodRevenue = billed.stream().map(r -> asBig(r[2])).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("MRR estimado", ReportFormatter.currency(mrr), "trending_up", "green"),
                ReportKpi.of("Ingreso del periodo", ReportFormatter.currency(periodRevenue), "payments", "blue"),
                ReportKpi.of("Cuentas con plan", ReportFormatter.number(accounts), "workspaces", "purple"),
                ReportKpi.of("ARPA", ReportFormatter.currency(arpa), "account_balance", "orange")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> planBars = byPlan.stream()
                .map(r -> new ReportBar(asString(r[0]), asBig(r[2]).doubleValue(), ReportFormatter.currency(asBig(r[2]))))
                .toList();
        if (!planBars.isEmpty()) {
            sections.add(new BarsSection("Ingreso por plan", planBars));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : billed) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("plan", asString(r[0]));
            row.put("org", asString(r[1]));
            row.put("amount", asBig(r[2]));
            row.put("date", asString(r[3]));
            rows.add(row);
        }
        sections.add(new TableSection("Suscripciones facturadas", List.of(
                ReportColumn.text("plan", "Plan"),
                ReportColumn.text("org", "Organización"),
                ReportColumn.currency("amount", "Monto"),
                ReportColumn.date("date", "Fecha")
        ), rows));

        return new ReportResult(key(), "Ingresos por Suscripciones",
                "Montos estimados a partir del precio del plan.", generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
