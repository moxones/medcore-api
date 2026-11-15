package com.medical.medcore.report.provider.clinic;

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
import com.medical.medcore.schedule.service.ScheduleCapacity;
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
public class ClinicBranchUtilizationProvider extends BaseReportProvider implements ReportProvider {

    private final ScheduleCapacity scheduleCapacity;
    private final ReportingAppointmentRepository appointments;

    @Override
    public String key() {
        return "clinic-branch-utilization";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.ADMIN, RoleConstants.SUPER_ADMIN);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        BranchFilter bf = resolveBranches(q, ctx);
        Long t = ctx.tenantId();

        Map<Long, String> branchNames = new LinkedHashMap<>();
        Map<Long, Long> capacity = new LinkedHashMap<>();
        Map<Long, Long> unavailableMinutes = new LinkedHashMap<>();
        Map<Long, Long> extraMinutes = new LinkedHashMap<>();
        for (ScheduleCapacity.Usage usage : scheduleCapacity.usage(q.from(), q.to(),
                bf.apply() ? bf.branchIds() : null, null)) {
            branchNames.putIfAbsent(usage.branchId(), usage.branchName());
            capacity.merge(usage.branchId(), usage.slots(), Long::sum);
            unavailableMinutes.merge(usage.branchId(), usage.blockedMinutes() + usage.closedMinutes(), Long::sum);
            extraMinutes.merge(usage.branchId(), usage.extraMinutes(), Long::sum);
        }

        Map<Long, Long> occupied = new LinkedHashMap<>();
        for (Object[] r : appointments.occupiedByBranch(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())) {
            long branchId = asLong(r[0]);
            branchNames.putIfAbsent(branchId, asString(r[1]));
            occupied.merge(branchId, asLong(r[2]), Long::sum);
        }

        long totalCapacity = capacity.values().stream().mapToLong(Long::longValue).sum();
        long totalOccupied = occupied.values().stream().mapToLong(Long::longValue).sum();
        long free = Math.max(0, totalCapacity - totalOccupied);
        double pct = ReportFormatter.ratio(totalOccupied, totalCapacity);

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Slots disponibles", ReportFormatter.number(totalCapacity), "event_available", "blue"),
                ReportKpi.of("Slots ocupados", ReportFormatter.number(totalOccupied), "event_busy", "green"),
                ReportKpi.of("% de ocupación", ReportFormatter.percent(pct), "donut_large", "purple"),
                ReportKpi.of("Slots libres", ReportFormatter.number(free), "event_note", "orange")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> bars = new ArrayList<>();
        List<Map<String, Object>> table = new ArrayList<>();
        for (Map.Entry<Long, String> e : branchNames.entrySet()) {
            long cap = capacity.getOrDefault(e.getKey(), 0L);
            long occ = occupied.getOrDefault(e.getKey(), 0L);
            double branchPct = ReportFormatter.ratio(occ, cap);
            bars.add(new ReportBar(e.getValue(), branchPct, ReportFormatter.percent(branchPct)));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("branch", e.getValue());
            row.put("capacity", cap);
            row.put("occupied", occ);
            row.put("pct", branchPct);
            row.put("unavailable", hours(unavailableMinutes.getOrDefault(e.getKey(), 0L)));
            row.put("extra", hours(extraMinutes.getOrDefault(e.getKey(), 0L)));
            table.add(row);
        }
        if (!bars.isEmpty()) {
            sections.add(new BarsSection("% ocupación por sucursal", "%", bars));
        }
        sections.add(new TableSection("Ocupación por sucursal", List.of(
                ReportColumn.text("branch", "Sucursal"),
                ReportColumn.number("capacity", "Capacidad"),
                ReportColumn.number("occupied", "Ocupados"),
                ReportColumn.percent("pct", "% Ocupación"),
                ReportColumn.number("unavailable", "Horas no disponibles"),
                ReportColumn.number("extra", "Horas extra")
        ), table));

        return new ReportResult(key(), "Ocupación de Agenda",
                "Capacidad según el horario efectivo: descuenta ausencias y feriados y suma horas extra.", generatedAt(),
                rangeOf(q), kpis, sections);
    }

    private double hours(long minutes) {
        return Math.round(minutes / 6.0) / 10.0;
    }
}
