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
import com.medical.medcore.repository.report.ReportingRescheduleRepository;
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
}
