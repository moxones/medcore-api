package com.medical.medcore.report.provider.clinic;

import com.medical.medcore.platform.domain.BusinessType;
import com.medical.medcore.platform.service.TenantTerminology;
import com.medical.medcore.report.dto.BarsSection;
import com.medical.medcore.report.dto.ReportBar;
import com.medical.medcore.report.dto.ReportColumn;
import com.medical.medcore.report.dto.ReportKpi;
import com.medical.medcore.report.dto.ReportResult;
import com.medical.medcore.report.dto.ReportSection;
import com.medical.medcore.report.dto.TableSection;
import com.medical.medcore.report.ReportContext;
import com.medical.medcore.report.ReportProvider;
import com.medical.medcore.report.ReportQuery;
import com.medical.medcore.report.provider.BaseReportProvider;
import com.medical.medcore.report.support.BranchFilter;
import com.medical.medcore.report.support.ReportFormatter;
import com.medical.medcore.report.repository.ReportingAppointmentRepository;
import com.medical.medcore.report.repository.ReportingPaymentRepository;
import com.medical.medcore.shared.security.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ClinicDoctorProductivityProvider extends BaseReportProvider implements ReportProvider {

    private static final long ALL_DOCTORS = -1L;

    private final ReportingAppointmentRepository appointments;
    private final ReportingPaymentRepository payments;
    private final TenantTerminology terminology;

    @Override
    public String key() {
        return "clinic-doctor-productivity";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.ADMIN, RoleConstants.SUPER_ADMIN);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        BranchFilter bf = resolveBranches(q, ctx);
        Long t = ctx.tenantId();
        BusinessType terms = terminology.current();
        Long doctorId = q.doctorId() != null ? q.doctorId() : ALL_DOCTORS;

        List<Object[]> rows = appointments.doctorProductivity(
                t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds(), doctorId);

        Map<Long, BigDecimal> revenueByDoctor = new HashMap<>();
        for (Object[] r : payments.revenueByDoctor(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())) {
            if (r[0] != null) {
                revenueByDoctor.put(asLong(r[0]), asBig(r[1]));
            }
        }

        long activeDoctors = rows.size();
        long totalCompleted = rows.stream().mapToLong(r -> asLong(r[3])).sum();
        BigDecimal totalRevenue = revenueByDoctor.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Double avgMinutes = appointments.avgConsultationMinutes(
                t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds(), doctorId);

        List<ReportKpi> kpis = List.of(
                ReportKpi.of(terms.professionalsLabel() + " activos", ReportFormatter.number(activeDoctors), "stethoscope", "blue"),
                ReportKpi.of("Consultas atendidas", ReportFormatter.number(totalCompleted), "task_alt", "green"),
                ReportKpi.of("Tiempo prom. consulta", ReportFormatter.minutes(Math.round(avgMinutes != null ? avgMinutes : 0)), "timer", "purple"),
                ReportKpi.of("Ingresos generados", ReportFormatter.currency(totalRevenue), "payments", "orange")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> consultBars = rows.stream()
                .limit(15)
                .map(r -> {
                    long c = asLong(r[2]);
                    return new ReportBar(asString(r[1]), c, ReportFormatter.number(c));
                })
                .toList();
        if (!consultBars.isEmpty()) {
            sections.add(new BarsSection("Atenciones por " + terms.professional(), consultBars));
        }

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("doctor", asString(r[1]));
            row.put("appointments", asLong(r[2]));
            row.put("noShows", asLong(r[4]));
            row.put("revenue", revenueByDoctor.getOrDefault(asLong(r[0]), BigDecimal.ZERO));
            ranking.add(row);
        }
        sections.add(new TableSection("Ranking de " + terms.professionals(), List.of(
                ReportColumn.text("doctor", terms.professionalLabel()),
                ReportColumn.number("appointments", "Citas"),
                ReportColumn.number("noShows", "No-shows"),
                ReportColumn.currency("revenue", "Ingresos")
        ), ranking));

        return new ReportResult(key(), "Productividad por " + terms.professional(), null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
