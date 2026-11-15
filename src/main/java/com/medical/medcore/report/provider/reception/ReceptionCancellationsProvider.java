package com.medical.medcore.report.provider.reception;

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
import com.medical.medcore.report.repository.ReportingRescheduleRepository;
import com.medical.medcore.shared.security.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReceptionCancellationsProvider extends BaseReportProvider implements ReportProvider {

    private static final long ALL_DOCTORS = -1L;

    private final ReportingAppointmentRepository appointments;
    private final ReportingRescheduleRepository reschedules;

    @Override
    public String key() {
        return "reception-cancellations";
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

        long total = 0, cancelled = 0, noShow = 0;
        List<ReportBar> noShowByDay = new ArrayList<>();
        for (Object[] r : daily) {
            total += asLong(r[1]);
            cancelled += asLong(r[3]);
            long dNoShow = asLong(r[4]);
            noShow += dNoShow;
            noShowByDay.add(new ReportBar(asString(r[0]), dNoShow, ReportFormatter.number(dNoShow)));
        }

        long rescheduled = reschedules.countInRange(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Cancelaciones", ReportFormatter.number(cancelled), "cancel", "orange"),
                ReportKpi.of("No-shows", ReportFormatter.number(noShow), "person_off", "red"),
                ReportKpi.of("Tasa de no-show", ReportFormatter.percent(ReportFormatter.ratio(noShow, total)), "percent", "purple"),
                ReportKpi.of("Reprogramadas", ReportFormatter.number(rescheduled), "event_repeat", "blue")
        );

        List<ReportSection> sections = new ArrayList<>();
        if (!noShowByDay.isEmpty()) {
            sections.add(new BarsSection("No-show por día", noShowByDay));
        }

        List<Map<String, Object>> originRows = new ArrayList<>();
        for (Object[] r : appointments.cancellationOrigins(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("origin", originLabel(asString(r[0])));
            row.put("count", asLong(r[1]));
            originRows.add(row);
        }
        sections.add(new TableSection("Cancelaciones por origen", List.of(
                ReportColumn.text("origin", "Quién canceló"),
                ReportColumn.number("count", "Cantidad")
        ), originRows));

        List<Map<String, Object>> reasonRows = new ArrayList<>();
        for (Object[] r : reschedules.reasonBreakdown(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("reason", asString(r[0]));
            row.put("count", asLong(r[1]));
            reasonRows.add(row);
        }
        sections.add(new TableSection("Motivos de reprogramación", List.of(
                ReportColumn.text("reason", "Motivo"),
                ReportColumn.number("count", "Cantidad")
        ), reasonRows));

        return new ReportResult(key(), "Ausencias y Cancelaciones",
                "Los motivos corresponden a reprogramaciones registradas.", generatedAt(),
                rangeOf(q), kpis, sections);
    }

    private String originLabel(String origin) {
        return switch (origin) {
            case "PATIENT" -> "Paciente";
            case "PROFESSIONAL" -> "Ausencia del profesional";
            case "ORGANIZATION" -> "Cierre de la sede";
            default -> "Sin especificar";
        };
    }
}
