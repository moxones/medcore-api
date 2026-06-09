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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PlatformOrganizationsProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingPlatformRepository platform;

    @Override
    public String key() {
        return "platform-organizations";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.SUPER_ADMIN);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        long active = 0, totalTenants = 0;
        List<Object[]> totals = platform.tenantTotals();
        if (!totals.isEmpty()) {
            active = asLong(totals.get(0)[0]);
            totalTenants = asLong(totals.get(0)[1]);
        }
        long signups = platform.tenantSignups(q.fromDateTime(), q.toDateTimeExclusive());
        long inactive = Math.max(0, totalTenants - active);
        double retention = ReportFormatter.ratio(active, totalTenants);

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Tenants activos", ReportFormatter.number(active), "domain", "green"),
                ReportKpi.of("Altas en el periodo", ReportFormatter.number(signups), "add_business", "blue"),
                ReportKpi.of("Bajas/cancelados", ReportFormatter.number(inactive), "domain_disabled", "orange"),
                ReportKpi.of("Retención", ReportFormatter.percent(retention), "verified", "purple")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> monthBars = platform.signupsByMonth(q.fromDateTime(), q.toDateTimeExclusive())
                .stream()
                .map(r -> {
                    long c = asLong(r[1]);
                    return new ReportBar(asString(r[0]), c, ReportFormatter.number(c));
                })
                .toList();
        if (!monthBars.isEmpty()) {
            sections.add(new BarsSection("Altas por mes", monthBars));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : platform.recentOrganizations()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", asString(r[0]));
            row.put("plan", asString(r[1]));
            row.put("status", asString(r[2]));
            row.put("createdAt", asString(r[3]));
            rows.add(row);
        }
        sections.add(new TableSection("Organizaciones recientes", List.of(
                ReportColumn.text("name", "Organización"),
                ReportColumn.text("plan", "Plan"),
                ReportColumn.text("status", "Estado"),
                ReportColumn.date("createdAt", "Fecha de alta")
        ), rows));

        return new ReportResult(key(), "Organizaciones y Crecimiento", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
