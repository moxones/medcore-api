package com.medical.medcore.report.provider.patient;

import com.medical.medcore.dto.response.report.BarsSection;
import com.medical.medcore.dto.response.report.ListSection;
import com.medical.medcore.dto.response.report.ReportBar;
import com.medical.medcore.dto.response.report.ReportKpi;
import com.medical.medcore.dto.response.report.ReportListItem;
import com.medical.medcore.dto.response.report.ReportResult;
import com.medical.medcore.dto.response.report.ReportSection;
import com.medical.medcore.report.ReportContext;
import com.medical.medcore.report.ReportProvider;
import com.medical.medcore.report.ReportQuery;
import com.medical.medcore.report.provider.BaseReportProvider;
import com.medical.medcore.report.support.CurrentUserResolver;
import com.medical.medcore.report.support.ReportFormatter;
import com.medical.medcore.repository.report.ReportingPatientRepository;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** "Resumen de Salud" — signos vitales del paciente autenticado. */
@Component
@RequiredArgsConstructor
public class PatientHealthSummaryProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingPatientRepository patientReports;
    private final CurrentUserResolver currentUser;

    @Override
    public String key() {
        return "patient-health-summary";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.PATIENT);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        Long patientId = currentUser.resolvePatientId(ctx);
        Long t = ctx.tenantId();

        // [date, weight, bmi, blood_pressure, heart_rate, temperature] ordenado ascendente.
        List<Object[]> vitals = patientReports.vitalsHistory(t, patientId, q.fromDateTime(), q.toDateTimeExclusive());
        Object[] latest = vitals.isEmpty() ? null : vitals.get(vitals.size() - 1);

        List<ReportKpi> kpis = List.of(
                new ReportKpi("Última presión", latest != null ? orDash(latest[3]) : "—", "monitor_heart", null, "red", null),
                new ReportKpi("Último peso", latest != null && latest[1] != null ? ReportFormatter.number(asDouble(latest[1])) + " kg" : "—", "scale", null, "blue", null),
                new ReportKpi("IMC", latest != null && latest[2] != null ? ReportFormatter.number(asDouble(latest[2])) : "—", "straighten", null, "purple", null),
                new ReportKpi("Última consulta", latest != null ? orDash(latest[0]) : "—", "event", null, "green", null)
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> weightBars = vitals.stream()
                .filter(r -> r[1] != null)
                .map(r -> {
                    double w = asDouble(r[1]);
                    return new ReportBar(asString(r[0]), w, ReportFormatter.number(w) + " kg");
                })
                .toList();
        if (!weightBars.isEmpty()) {
            sections.add(new BarsSection("Peso en el tiempo", "kg", weightBars));
        }

        if (latest != null) {
            String date = orDash(latest[0]);
            List<ReportListItem> items = new ArrayList<>();
            if (latest[1] != null) {
                items.add(new ReportListItem("Peso", ReportFormatter.number(asDouble(latest[1])) + " kg", date));
            }
            if (latest[2] != null) {
                items.add(new ReportListItem("IMC", ReportFormatter.number(asDouble(latest[2])), date));
            }
            if (latest[3] != null) {
                items.add(new ReportListItem("Presión arterial", asString(latest[3]), date));
            }
            if (latest[4] != null) {
                items.add(new ReportListItem("Frecuencia cardiaca", asLong(latest[4]) + " lpm", date));
            }
            if (latest[5] != null) {
                items.add(new ReportListItem("Temperatura", ReportFormatter.number(asDouble(latest[5])) + " °C", date));
            }
            if (!items.isEmpty()) {
                sections.add(new ListSection("Últimas mediciones", items));
            }
        }

        return new ReportResult(key(), "Resumen de Salud", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }

    private String orDash(Object o) {
        String s = asString(o);
        return s.isBlank() ? "—" : s;
    }
}
