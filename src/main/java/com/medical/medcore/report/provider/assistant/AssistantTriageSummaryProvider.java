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
import com.medical.medcore.repository.report.ReportingTriageRepository;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AssistantTriageSummaryProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingTriageRepository triage;

    @Override
    public String key() {
        return "assistant-triage-summary";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.ASSISTANT);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        BranchFilter bf = resolveBranches(q, ctx);
        Long t = ctx.tenantId();

        long totalTriages = 0, patients = 0, highPriority = 0;
        List<Object[]> summary = triage.summary(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds());
        if (!summary.isEmpty()) {
            totalTriages = asLong(summary.get(0)[0]);
            patients = asLong(summary.get(0)[1]);
            highPriority = asLong(summary.get(0)[2]);
        }

        long days = Math.max(1, ChronoUnit.DAYS.between(q.from(), q.to()) + 1);
        double perDay = (double) totalTriages / days;

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Triajes realizados", ReportFormatter.number(totalTriages), "vital_signs", "blue"),
                ReportKpi.of("Pacientes triados", ReportFormatter.number(patients), "groups", "purple"),
                ReportKpi.of("Prioridad alta", ReportFormatter.number(highPriority), "priority_high", "red"),
                ReportKpi.of("Promedio por día", ReportFormatter.number(perDay), "calendar_today", "green")
        );

        List<ReportSection> sections = new ArrayList<>();

        List<ReportBar> priorityBars = triage.byPriority(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())
                .stream()
                .map(r -> {
                    long c = asLong(r[1]);
                    return new ReportBar(asString(r[0]), c, ReportFormatter.number(c));
                })
                .toList();
        if (!priorityBars.isEmpty()) {
            sections.add(new BarsSection("Triajes por prioridad", priorityBars));
        }

        List<Map<String, Object>> dayRows = new ArrayList<>();
        for (Object[] r : triage.dailyByPriority(t, q.fromDateTime(), q.toDateTimeExclusive(), bf.apply(), bf.branchIds())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", asString(r[0]));
            row.put("total", asLong(r[1]));
            row.put("high", asLong(r[2]));
            row.put("medium", asLong(r[3]));
            row.put("low", asLong(r[4]));
            dayRows.add(row);
        }
        sections.add(new TableSection("Triajes por día", List.of(
                ReportColumn.date("date", "Fecha"),
                ReportColumn.number("total", "Total"),
                ReportColumn.number("high", "Alta"),
                ReportColumn.number("medium", "Media"),
                ReportColumn.number("low", "Baja")
        ), dayRows));

        return new ReportResult(key(), "Resumen de Triaje", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
