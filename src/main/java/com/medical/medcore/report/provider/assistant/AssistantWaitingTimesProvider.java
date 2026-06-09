package com.medical.medcore.report.provider.assistant;

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

/**
 * Tiempos de espera = inicio de atención (started_at) − check-in (checked_in_at), tomados de
 * las transiciones de la sala de espera.
 */
@Component
@RequiredArgsConstructor
public class AssistantWaitingTimesProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingAppointmentRepository appointments;

    @Override
    public String key() {
        return "assistant-waiting-times";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.ASSISTANT);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        BranchFilter bf = resolveBranches(q, ctx);
        Long t = ctx.tenantId();

        double avg = 0, max = 0;
        long attended = 0;
        List<Object[]> stats = appointments.waitingTimeStats(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        if (!stats.isEmpty()) {
            avg = asDouble(stats.get(0)[0]);
            max = asDouble(stats.get(0)[1]);
            attended = asLong(stats.get(0)[2]);
        }

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Espera promedio", ReportFormatter.minutes(Math.round(avg)), "timer", "blue"),
                ReportKpi.of("Espera máxima", ReportFormatter.minutes(Math.round(max)), "timelapse", "red"),
                ReportKpi.of("Pacientes atendidos", ReportFormatter.number(attended), "groups", "green")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> hourBars = appointments.waitingTimeByHour(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())
                .stream()
                .map(r -> {
                    double mins = asDouble(r[1]);
                    return new ReportBar(asString(r[0]), mins, ReportFormatter.minutes(Math.round(mins)));
                })
                .toList();
        if (!hourBars.isEmpty()) {
            sections.add(new BarsSection("Espera promedio por franja horaria", "min", hourBars));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ReportBar b : hourBars) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("slot", b.label());
            row.put("avg", Math.round(b.value()));
            rows.add(row);
        }
        sections.add(new TableSection("Detalle por franja", List.of(
                ReportColumn.text("slot", "Franja"),
                ReportColumn.number("avg", "Espera promedio (min)")
        ), rows));

        return new ReportResult(key(), "Tiempos de Espera", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
