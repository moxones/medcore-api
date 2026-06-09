package com.medical.medcore.report.provider.clinic;

import com.medical.medcore.dto.response.report.BarsSection;
import com.medical.medcore.dto.response.report.ReportBar;
import com.medical.medcore.dto.response.report.ReportKpi;
import com.medical.medcore.dto.response.report.ReportResult;
import com.medical.medcore.dto.response.report.ReportSection;
import com.medical.medcore.report.ReportContext;
import com.medical.medcore.report.ReportProvider;
import com.medical.medcore.report.ReportQuery;
import com.medical.medcore.report.provider.BaseReportProvider;
import com.medical.medcore.report.support.BranchFilter;
import com.medical.medcore.report.support.ReportFormatter;
import com.medical.medcore.repository.report.ReportingAppointmentRepository;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClinicPatientInsightsProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingAppointmentRepository appointments;

    @Override
    public String key() {
        return "clinic-patient-insights";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.ADMIN, RoleConstants.SUPER_ADMIN);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        BranchFilter bf = resolveBranches(q, ctx);
        Long t = ctx.tenantId();

        long totalSeen = appointments.distinctPatients(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        long newPatients = appointments.newPatients(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        long returning = Math.max(0, totalSeen - newPatients);
        Double avgAge = appointments.averageAge(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Pacientes nuevos", ReportFormatter.number(newPatients), "person_add", "green"),
                ReportKpi.of("Recurrentes", ReportFormatter.number(returning), "groups", "blue"),
                ReportKpi.of("Total atendidos", ReportFormatter.number(totalSeen), "diversity_3", "purple"),
                ReportKpi.of("Edad promedio", avgAge != null ? Math.round(avgAge) + " años" : "—", "cake", "orange")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> ageBars = appointments.ageGroups(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())
                .stream()
                .map(r -> {
                    long c = asLong(r[1]);
                    return new ReportBar(asString(r[0]), c, ReportFormatter.number(c));
                })
                .toList();
        if (!ageBars.isEmpty()) {
            sections.add(new BarsSection("Pacientes por grupo de edad", ageBars));
        }

        sections.add(new BarsSection("Nuevos vs recurrentes", List.of(
                new ReportBar("Nuevos", newPatients, ReportFormatter.number(newPatients)),
                new ReportBar("Recurrentes", returning, ReportFormatter.number(returning))
        )));

        return new ReportResult(key(), "Análisis de Pacientes", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
