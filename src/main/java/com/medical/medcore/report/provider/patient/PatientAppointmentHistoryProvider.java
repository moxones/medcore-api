package com.medical.medcore.report.provider.patient;

import com.medical.medcore.dto.response.report.ReportColumn;
import com.medical.medcore.dto.response.report.ReportKpi;
import com.medical.medcore.dto.response.report.ReportResult;
import com.medical.medcore.dto.response.report.ReportSection;
import com.medical.medcore.dto.response.report.TableSection;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** "Mi Historial de Citas" — acotado al paciente autenticado. */
@Component
@RequiredArgsConstructor
public class PatientAppointmentHistoryProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingPatientRepository patientReports;
    private final CurrentUserResolver currentUser;

    @Override
    public String key() {
        return "patient-appointment-history";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.PATIENT);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        Long patientId = currentUser.resolvePatientId(ctx);
        Long t = ctx.tenantId();

        long total = 0, attended = 0, cancelled = 0, doctors = 0;
        List<Object[]> summary = patientReports.appointmentSummary(t, patientId, q.fromDateTime(), q.toDateTimeExclusive());
        if (!summary.isEmpty()) {
            Object[] r = summary.get(0);
            total = asLong(r[0]);
            attended = asLong(r[1]);
            cancelled = asLong(r[2]);
            doctors = asLong(r[3]);
        }

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Citas totales", ReportFormatter.number(total), "event", "blue"),
                ReportKpi.of("Atendidas", ReportFormatter.number(attended), "task_alt", "green"),
                ReportKpi.of("Canceladas", ReportFormatter.number(cancelled), "cancel", "orange"),
                ReportKpi.of("Médicos distintos", ReportFormatter.number(doctors), "stethoscope", "purple")
        );

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : patientReports.appointmentHistory(t, patientId, q.fromDateTime(), q.toDateTimeExclusive())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", asString(r[0]));
            row.put("doctor", asString(r[1]));
            row.put("specialty", asString(r[2]));
            row.put("status", asString(r[3]));
            rows.add(row);
        }
        List<ReportSection> sections = List.of(new TableSection("Mis citas", List.of(
                ReportColumn.date("date", "Fecha"),
                ReportColumn.text("doctor", "Médico"),
                ReportColumn.text("specialty", "Especialidad"),
                ReportColumn.text("status", "Estado")
        ), rows));

        return new ReportResult(key(), "Mi Historial de Citas", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
