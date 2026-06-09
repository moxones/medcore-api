package com.medical.medcore.report.provider.reception;

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
public class ReceptionDailyAppointmentsProvider extends BaseReportProvider implements ReportProvider {

    private static final long ALL_DOCTORS = -1L;

    private final ReportingAppointmentRepository appointments;

    @Override
    public String key() {
        return "reception-daily-appointments";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.RECEPTIONIST);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        BranchFilter bf = resolveBranches(q, ctx);
        Long t = ctx.tenantId();

        List<Object[]> daily = appointments.dailyStats(
                t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds(), ALL_DOCTORS);

        long total = 0, attended = 0, cancelled = 0, noShow = 0;
        List<Map<String, Object>> dayRows = new ArrayList<>();
        for (Object[] r : daily) {
            long dTotal = asLong(r[1]);
            long dAttended = asLong(r[2]);
            long dCancelled = asLong(r[3]);
            long dNoShow = asLong(r[4]);
            total += dTotal;
            attended += dAttended;
            cancelled += dCancelled;
            noShow += dNoShow;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", asString(r[0]));
            row.put("scheduled", dTotal);
            row.put("attended", dAttended);
            row.put("cancelled", dCancelled);
            row.put("noShow", dNoShow);
            dayRows.add(row);
        }

        long waiting = appointments.flowBreakdown(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())
                .stream()
                .filter(r -> {
                    String f = asString(r[0]);
                    return "WAITING".equals(f) || "CALLED".equals(f);
                })
                .mapToLong(r -> asLong(r[1]))
                .sum();

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Agendadas", ReportFormatter.number(total), "event", "blue"),
                ReportKpi.of("Atendidas", ReportFormatter.number(attended), "task_alt", "green"),
                ReportKpi.of("En espera", ReportFormatter.number(waiting), "hourglass_top", "orange"),
                ReportKpi.of("No-show", ReportFormatter.number(noShow), "person_off", "red")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> statusBars = appointments.statusBreakdown(
                        t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds(), ALL_DOCTORS)
                .stream()
                .map(r -> {
                    long c = asLong(r[2]);
                    return new ReportBar(asString(r[1]), c, ReportFormatter.number(c));
                })
                .toList();
        if (!statusBars.isEmpty()) {
            sections.add(new BarsSection("Citas por estado", statusBars));
        }

        sections.add(new TableSection("Citas por día", List.of(
                ReportColumn.date("date", "Fecha"),
                ReportColumn.number("scheduled", "Agendadas"),
                ReportColumn.number("attended", "Atendidas"),
                ReportColumn.number("cancelled", "Canceladas"),
                ReportColumn.number("noShow", "No-show")
        ), dayRows));

        return new ReportResult(key(), "Citas por Día", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
