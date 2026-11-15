package com.medical.medcore.report.provider.clinic;

import com.medical.medcore.platform.service.TenantTerminology;
import com.medical.medcore.report.ReportContext;
import com.medical.medcore.report.ReportProvider;
import com.medical.medcore.report.ReportQuery;
import com.medical.medcore.report.dto.BarsSection;
import com.medical.medcore.report.dto.ReportBar;
import com.medical.medcore.report.dto.ReportColumn;
import com.medical.medcore.report.dto.ReportKpi;
import com.medical.medcore.report.dto.ReportResult;
import com.medical.medcore.report.dto.ReportSection;
import com.medical.medcore.report.dto.TableSection;
import com.medical.medcore.report.provider.BaseReportProvider;
import com.medical.medcore.report.repository.ReportingAppointmentRepository;
import com.medical.medcore.report.support.BranchFilter;
import com.medical.medcore.report.support.ReportFormatter;
import com.medical.medcore.schedule.domain.ExceptionKind;
import com.medical.medcore.schedule.domain.ExceptionReason;
import com.medical.medcore.schedule.domain.ExceptionStatus;
import com.medical.medcore.schedule.domain.ScheduleException;
import com.medical.medcore.schedule.repository.ScheduleExceptionRepository;
import com.medical.medcore.schedule.service.ScheduleCapacity;
import com.medical.medcore.shared.security.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicScheduleChangesProvider extends BaseReportProvider implements ReportProvider {

    private static final long ALL_DOCTORS = -1L;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ScheduleCapacity scheduleCapacity;
    private final ScheduleExceptionRepository exceptionRepository;
    private final ReportingAppointmentRepository appointments;
    private final TenantTerminology terminology;

    @Override
    public String key() {
        return "clinic-schedule-changes";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.ADMIN, RoleConstants.SUPER_ADMIN);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        BranchFilter bf = resolveBranches(q, ctx);
        Long doctorId = q.doctorId();

        Map<Long, long[]> byDoctor = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        Map<ExceptionReason, Long> blockedByReason = new EnumMap<>(ExceptionReason.class);
        for (ScheduleCapacity.Usage usage : scheduleCapacity.usage(q.from(), q.to(),
                bf.apply() ? bf.branchIds() : null, doctorId)) {
            names.putIfAbsent(usage.doctorId(), usage.doctorName());
            long[] totals = byDoctor.computeIfAbsent(usage.doctorId(), key -> new long[7]);
            totals[0] += usage.templateMinutes();
            totals[1] += usage.blockedMinutes();
            totals[2] += usage.extraMinutes();
            totals[3] += usage.closedMinutes();
            usage.blockedByReason().forEach((reason, minutes) -> blockedByReason.merge(reason, minutes, Long::sum));
        }
        for (Object[] row : appointments.scheduleChangeImpact(ctx.tenantId(), q.from(), q.to(), bf.apply(),
                bf.branchIds(), doctorId != null ? doctorId : ALL_DOCTORS)) {
            long[] totals = byDoctor.computeIfAbsent(asLong(row[0]), key -> new long[7]);
            totals[4] += asLong(row[1]);
            totals[5] += asLong(row[2]);
            totals[6] += asLong(row[3]);
        }

        List<ScheduleException> changes = exceptionRepository.search(ctx.tenantId(), doctorId, null, q.from(), q.to())
                .stream()
                .filter(exception -> exception.getStatus() != ExceptionStatus.CANCELLED
                        && exception.getStatus() != ExceptionStatus.REJECTED)
                .filter(exception -> !bf.apply() || exception.getBranch() == null
                        || bf.branchIds().contains(exception.branchId()))
                .toList();
        for (ScheduleException exception : changes) {
            names.putIfAbsent(exception.getDoctor().getId(),
                    terminology.professionalName(exception.getDoctor().getPerson()));
        }

        long absences = changes.stream().filter(exception -> exception.getKind() == ExceptionKind.BLOCK).count();
        long blocked = byDoctor.values().stream().mapToLong(t -> t[1]).sum();
        long extra = byDoctor.values().stream().mapToLong(t -> t[2]).sum();
        long affected = byDoctor.values().stream().mapToLong(t -> t[4]).sum();
        long cancelled = byDoctor.values().stream().mapToLong(t -> t[5]).sum();
        long pending = byDoctor.values().stream().mapToLong(t -> t[6]).sum();

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Ausencias registradas", ReportFormatter.number(absences), "event_busy", "orange"),
                ReportKpi.of("Horas no disponibles", ReportFormatter.number(hours(blocked)), "schedule", "red"),
                ReportKpi.of("Horas extra", ReportFormatter.number(hours(extra)), "more_time", "green"),
                ReportKpi.of("Citas afectadas", ReportFormatter.number(affected), "event_repeat", "blue"),
                ReportKpi.of("Canceladas por ausencia", ReportFormatter.number(cancelled), "cancel", "purple"),
                ReportKpi.of("Por reprogramar", ReportFormatter.number(pending), "pending_actions", "orange")
        );

        List<ReportSection> sections = new ArrayList<>();
        List<ReportBar> reasonBars = new ArrayList<>();
        blockedByReason.forEach((reason, minutes) -> reasonBars.add(
                new ReportBar(reason.label(), hours(minutes), ReportFormatter.number(hours(minutes)) + " h")));
        if (!reasonBars.isEmpty()) {
            sections.add(new BarsSection("Horas no disponibles por motivo", "h", reasonBars));
        }

        List<Map<String, Object>> doctorRows = new ArrayList<>();
        byDoctor.forEach((id, totals) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("doctor", names.getOrDefault(id, "—"));
            row.put("planned", hours(totals[0]));
            row.put("blocked", hours(totals[1]));
            row.put("extra", hours(totals[2]));
            row.put("closed", hours(totals[3]));
            row.put("affected", totals[4]);
            row.put("cancelled", totals[5]);
            row.put("pending", totals[6]);
            doctorRows.add(row);
        });
        sections.add(new TableSection("Por profesional", List.of(
                ReportColumn.text("doctor", "Profesional"),
                ReportColumn.number("planned", "Horas habituales"),
                ReportColumn.number("blocked", "Horas no disponibles"),
                ReportColumn.number("extra", "Horas extra"),
                ReportColumn.number("closed", "Horas en feriados"),
                ReportColumn.number("affected", "Citas afectadas"),
                ReportColumn.number("cancelled", "Canceladas"),
                ReportColumn.number("pending", "Por reprogramar")
        ), doctorRows));

        List<Map<String, Object>> detailRows = new ArrayList<>();
        for (ScheduleException exception : changes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("doctor", names.getOrDefault(exception.getDoctor().getId(), "—"));
            row.put("kind", exception.getKind() == ExceptionKind.BLOCK ? "Ausencia" : "Horas extra");
            row.put("dates", exception.getStartDate().equals(exception.getEndDate())
                    ? DAY.format(exception.getStartDate())
                    : DAY.format(exception.getStartDate()) + " – " + DAY.format(exception.getEndDate()));
            row.put("hours", exception.isFullDay() ? "Todo el día"
                    : exception.getStartTime() + " – " + exception.getEndTime());
            row.put("branch", exception.getBranch() != null ? exception.getBranch().getName() : "Todas");
            row.put("reason", exception.getReason().label());
            row.put("status", exception.getStatus() == ExceptionStatus.PENDING ? "Por aprobar" : "Vigente");
            detailRows.add(row);
        }
        sections.add(new TableSection("Detalle de cambios", List.of(
                ReportColumn.text("doctor", "Profesional"),
                ReportColumn.text("kind", "Tipo"),
                ReportColumn.text("dates", "Fechas"),
                ReportColumn.text("hours", "Horario"),
                ReportColumn.text("branch", "Sede"),
                ReportColumn.text("reason", "Motivo"),
                ReportColumn.text("status", "Estado")
        ), detailRows));

        return new ReportResult(key(), "Ausencias y Horas Extra",
                "Horas calculadas con el horario habitual, las ausencias, los feriados y las horas extra.",
                generatedAt(), rangeOf(q), kpis, sections);
    }

    private double hours(long minutes) {
        return Math.round(minutes / 6.0) / 10.0;
    }
}
