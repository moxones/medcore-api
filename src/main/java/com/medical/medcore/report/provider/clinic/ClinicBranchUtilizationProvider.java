package com.medical.medcore.report.provider.clinic;

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
import com.medical.medcore.repository.report.ReportingScheduleRepository;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ocupación de agenda: estima la capacidad por sucursal a partir de los horarios médicos
 * (slots/día × ocurrencias del día de la semana en el rango) y la compara con las citas
 * efectivamente ocupadas. Es una estimación (best-effort).
 */
@Component
@RequiredArgsConstructor
public class ClinicBranchUtilizationProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingScheduleRepository schedules;
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

        // Capacidad estimada por sucursal (en slots).
        Map<Long, String> branchNames = new LinkedHashMap<>();
        Map<Long, Long> capacity = new LinkedHashMap<>();
        for (Object[] s : schedules.activeSchedules(t, q.from(), q.to(), bf.apply(), bf.branchIds())) {
            long branchId = asLong(s[0]);
            branchNames.putIfAbsent(branchId, asString(s[1]));
            int dayOfWeek = (int) asLong(s[2]);
            long slotsPerDay = slotsPerDay(s[3], s[4], (int) asLong(s[5]), (int) asLong(s[6]));
            long occurrences = weekdayOccurrences(q.from(), q.to(), dayOfWeek);
            capacity.merge(branchId, slotsPerDay * occurrences, Long::sum);
        }

        // Ocupación real por sucursal.
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
            table.add(row);
        }
        if (!bars.isEmpty()) {
            sections.add(new BarsSection("% ocupación por sucursal", "%", bars));
        }
        sections.add(new TableSection("Ocupación por sucursal", List.of(
                ReportColumn.text("branch", "Sucursal"),
                ReportColumn.number("capacity", "Capacidad"),
                ReportColumn.number("occupied", "Ocupados"),
                ReportColumn.percent("pct", "% Ocupación")
        ), table));

        return new ReportResult(key(), "Ocupación de Agenda",
                "Capacidad estimada a partir de los horarios médicos.", generatedAt(),
                rangeOf(q), kpis, sections);
    }

    private long slotsPerDay(Object startTime, Object endTime, int slotDurationMinutes, int maxPatientsPerSlot) {
        if (slotDurationMinutes <= 0 || !(startTime instanceof Time start) || !(endTime instanceof Time end)) {
            return 0;
        }
        long minutes = (end.getTime() - start.getTime()) / 60000L;
        if (minutes <= 0) {
            return 0;
        }
        long slots = minutes / slotDurationMinutes;
        return slots * Math.max(1, maxPatientsPerSlot);
    }

    /** Cuenta los días dentro de [from, to] cuyo día de la semana coincide con {@code dayOfWeek}. */
    private long weekdayOccurrences(LocalDate from, LocalDate to, int dayOfWeek) {
        long count = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (matchesDayOfWeek(d, dayOfWeek)) {
                count++;
            }
        }
        return count;
    }

    /** Tolera convenciones 1-7 (ISO: lun=1..dom=7) y 0-6 (dom=0..sáb=6). */
    private boolean matchesDayOfWeek(LocalDate date, int dayOfWeek) {
        int iso = date.getDayOfWeek().getValue(); // 1=lunes .. 7=domingo
        if (dayOfWeek >= 1 && dayOfWeek <= 7) {
            return iso == dayOfWeek;
        }
        if (dayOfWeek == 0) {
            return iso == 7; // domingo
        }
        return false;
    }
}
