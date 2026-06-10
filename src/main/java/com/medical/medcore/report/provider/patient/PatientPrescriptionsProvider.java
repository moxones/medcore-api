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

@Component
@RequiredArgsConstructor
public class PatientPrescriptionsProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingPatientRepository patientReports;
    private final CurrentUserResolver currentUser;

    @Override
    public String key() {
        return "patient-prescriptions";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.PATIENT);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        Long patientId = currentUser.resolvePatientId(ctx);
        Long t = ctx.tenantId();

        List<Object[]> prescriptions = patientReports.prescriptions(t, patientId, q.fromDateTime(), q.toDateTimeExclusive());

        long total = prescriptions.size();
        long active = prescriptions.stream().filter(r -> Boolean.TRUE.equals(r[4])).count();
        String last = prescriptions.isEmpty() ? "—" : asString(prescriptions.get(0)[3]);

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Recetas", ReportFormatter.number(total), "prescriptions", "blue"),
                ReportKpi.of("Medicamentos activos", ReportFormatter.number(active), "medication", "green"),
                new ReportKpi("Última receta", last, "event", null, "purple", null)
        );

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : prescriptions) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("medication", asString(r[0]));
            row.put("dosage", asString(r[1]));
            row.put("instructions", asString(r[2]));
            row.put("date", asString(r[3]));
            rows.add(row);
        }
        List<ReportSection> sections = List.of(new TableSection("Medicamentos prescritos", List.of(
                ReportColumn.text("medication", "Medicamento"),
                ReportColumn.text("dosage", "Dosis"),
                ReportColumn.text("instructions", "Indicación"),
                ReportColumn.date("date", "Fecha")
        ), rows));

        return new ReportResult(key(), "Mis Recetas", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
