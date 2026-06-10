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
import com.medical.medcore.report.support.CurrentUserResolver;
import com.medical.medcore.report.support.ReportFormatter;
import com.medical.medcore.repository.report.ReportingClinicalRepository;
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
public class DoctorDiagnosesProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingClinicalRepository clinical;
    private final CurrentUserResolver currentUser;

    @Override
    public String key() {
        return "doctor-diagnoses";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.DOCTOR);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        Long doctorId = currentUser.resolveDoctorId(ctx);
        Long t = ctx.tenantId();

        List<Object[]> top = clinical.topDiagnosesForDoctor(t, doctorId, q.fromDateTime(), q.toDateTimeExclusive());

        long total = 0;
        long distinct = 0;
        List<Object[]> summary = clinical.diagnosisSummaryForDoctor(t, doctorId, q.fromDateTime(), q.toDateTimeExclusive());
        if (!summary.isEmpty()) {
            total = asLong(summary.get(0)[0]);
            distinct = asLong(summary.get(0)[1]);
        }
        String mostFrequent = top.isEmpty() ? "—" : asString(top.get(0)[0]) + " " + asString(top.get(0)[1]);

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Diagnósticos registrados", ReportFormatter.number(total), "clinical_notes", "blue"),
                ReportKpi.of("CIE-10 distintos", ReportFormatter.number(distinct), "category", "purple"),
                new ReportKpi("Más frecuente", mostFrequent, "trending_up", null, "green", null)
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> bars = top.stream()
                .limit(10)
                .map(r -> {
                    long c = asLong(r[2]);
                    String label = asString(r[0]) + " · " + asString(r[1]);
                    return new ReportBar(label, c, ReportFormatter.number(c));
                })
                .toList();
        if (!bars.isEmpty()) {
            sections.add(new BarsSection("Top 10 diagnósticos CIE-10", bars));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : top) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", asString(r[0]));
            row.put("description", asString(r[1]));
            row.put("frequency", asLong(r[2]));
            rows.add(row);
        }
        sections.add(new TableSection("Detalle", List.of(
                ReportColumn.text("code", "CIE-10"),
                ReportColumn.text("description", "Descripción"),
                ReportColumn.number("frequency", "Frecuencia")
        ), rows));

        return new ReportResult(key(), "Diagnósticos Frecuentes", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
