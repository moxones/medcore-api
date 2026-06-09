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

/** "Recetas y Órdenes Emitidas" — acotado al médico autenticado. */
@Component
@RequiredArgsConstructor
public class DoctorPrescriptionsProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingClinicalRepository clinical;
    private final CurrentUserResolver currentUser;

    @Override
    public String key() {
        return "doctor-prescriptions";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.DOCTOR);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        Long doctorId = currentUser.resolveDoctorId(ctx);
        Long t = ctx.tenantId();

        long totalPrescriptions = 0, distinctMeds = 0;
        List<Object[]> summary = clinical.prescriptionSummaryForDoctor(t, doctorId, q.fromDateTime(), q.toDateTimeExclusive());
        if (!summary.isEmpty()) {
            totalPrescriptions = asLong(summary.get(0)[0]);
            distinctMeds = asLong(summary.get(0)[1]);
        }

        List<Object[]> ordersByType = clinical.ordersByTypeForDoctor(t, doctorId, q.fromDateTime(), q.toDateTimeExclusive());
        long totalOrders = ordersByType.stream().mapToLong(r -> asLong(r[1])).sum();

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Recetas emitidas", ReportFormatter.number(totalPrescriptions), "prescriptions", "blue"),
                ReportKpi.of("Medicamentos distintos", ReportFormatter.number(distinctMeds), "medication", "purple"),
                ReportKpi.of("Órdenes de examen", ReportFormatter.number(totalOrders), "science", "green")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> medBars = clinical.topMedicationsForDoctor(t, doctorId, q.fromDateTime(), q.toDateTimeExclusive())
                .stream()
                .limit(10)
                .map(r -> {
                    long c = asLong(r[1]);
                    return new ReportBar(asString(r[0]), c, ReportFormatter.number(c));
                })
                .toList();
        if (!medBars.isEmpty()) {
            sections.add(new BarsSection("Medicamentos más prescritos", medBars));
        }

        List<Map<String, Object>> orderRows = new ArrayList<>();
        for (Object[] r : ordersByType) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", asString(r[0]));
            row.put("count", asLong(r[1]));
            orderRows.add(row);
        }
        sections.add(new TableSection("Órdenes por tipo de examen", List.of(
                ReportColumn.text("type", "Tipo de examen"),
                ReportColumn.number("count", "Cantidad")
        ), orderRows));

        return new ReportResult(key(), "Recetas y Órdenes Emitidas", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
