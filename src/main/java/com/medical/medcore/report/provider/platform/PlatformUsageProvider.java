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
public class PlatformUsageProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingPlatformRepository platform;

    @Override
    public String key() {
        return "platform-usage";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.SUPER_ADMIN);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        long appointmentsCreated = platform.totalAppointmentsCreated(q.fromDateTime(), q.toDateTimeExclusive());
        long activeTenants = platform.tenantsWithActivity(q.fromDateTime(), q.toDateTimeExclusive());

        List<ReportKpi> kpis = List.of(
                new ReportKpi("Usuarios activos", "—", "group", "No disponible (falta registro de último acceso)", "blue", null),
                ReportKpi.of("Citas creadas", ReportFormatter.number(appointmentsCreated), "event", "green"),
                new ReportKpi("Logins", "—", "login", "No disponible (falta registro de último acceso)", "orange", null),
                ReportKpi.of("Tenants con actividad", ReportFormatter.number(activeTenants), "domain", "purple")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<Object[]> byTenant = platform.appointmentsByTenant(q.fromDateTime(), q.toDateTimeExclusive());

        List<ReportBar> bars = byTenant.stream()
                .limit(15)
                .map(r -> {
                    long c = asLong(r[1]);
                    return new ReportBar(asString(r[0]), c, ReportFormatter.number(c));
                })
                .toList();
        if (!bars.isEmpty()) {
            sections.add(new BarsSection("Citas creadas por organización", bars));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : byTenant) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("org", asString(r[0]));
            row.put("appointments", asLong(r[1]));
            rows.add(row);
        }
        sections.add(new TableSection("Actividad por organización", List.of(
                ReportColumn.text("org", "Organización"),
                ReportColumn.number("appointments", "Citas creadas")
        ), rows));

        return new ReportResult(key(), "Uso de la Plataforma", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
