package com.medical.medcore.report.provider.doctor;

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
import com.medical.medcore.report.support.CurrentUserResolver;
import com.medical.medcore.report.support.ReportFormatter;
import com.medical.medcore.repository.report.ReportingAppointmentRepository;
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
public class DoctorProductivityProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingAppointmentRepository appointments;
    private final CurrentUserResolver currentUser;

    @Override
    public String key() {
        return "doctor-productivity";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.DOCTOR);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        Long doctorId = currentUser.resolveDoctorId(ctx);
        Long t = ctx.tenantId();

        List<Object[]> prod = appointments.doctorProductivity(
                t, q.fromDateTime(), q.toDateTimeExclusive(), false, BranchFilter.none().branchIds(), doctorId);
        long completed = 0, noShow = 0;
        if (!prod.isEmpty()) {
            Object[] r = prod.get(0);
            completed = asLong(r[3]);
            noShow = asLong(r[4]);
        }
        long uniquePatients = appointments.distinctPatientsForDoctor(t, doctorId, q.fromDateTime(), q.toDateTimeExclusive());
        Double avgMinutes = appointments.avgConsultationMinutes(
                t, q.fromDateTime(), q.toDateTimeExclusive(), false, BranchFilter.none().branchIds(), doctorId);

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Consultas atendidas", ReportFormatter.number(completed), "task_alt", "green"),
                ReportKpi.of("Tiempo promedio", ReportFormatter.minutes(Math.round(avgMinutes != null ? avgMinutes : 0)), "timer", "blue"),
                ReportKpi.of("Ausencias (no-show)", ReportFormatter.number(noShow), "person_off", "red"),
                ReportKpi.of("Pacientes únicos", ReportFormatter.number(uniquePatients), "groups", "purple")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> dayBars = appointments.completedByDayForDoctor(t, doctorId, q.fromDateTime(), q.toDateTimeExclusive())
                .stream()
                .map(r -> {
                    long c = asLong(r[1]);
                    return new ReportBar(asString(r[0]), c, ReportFormatter.number(c));
                })
                .toList();
        if (!dayBars.isEmpty()) {
            sections.add(new BarsSection("Consultas por día", dayBars));
        }

        List<Map<String, Object>> specialtyRows = new ArrayList<>();
        for (Object[] r : appointments.specialtySummaryForDoctor(t, doctorId, q.fromDateTime(), q.toDateTimeExclusive())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("specialty", asString(r[0]));
            row.put("appointments", asLong(r[1]));
            row.put("noShows", asLong(r[2]));
            specialtyRows.add(row);
        }
        sections.add(new TableSection("Resumen por especialidad", List.of(
                ReportColumn.text("specialty", "Especialidad"),
                ReportColumn.number("appointments", "Citas"),
                ReportColumn.number("noShows", "No-shows")
        ), specialtyRows));

        return new ReportResult(key(), "Mi Productividad", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
