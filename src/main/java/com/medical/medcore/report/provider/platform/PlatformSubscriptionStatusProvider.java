package com.medical.medcore.report.provider.platform;

import com.medical.medcore.dto.response.report.ReportColumn;
import com.medical.medcore.dto.response.report.ReportKpi;
import com.medical.medcore.dto.response.report.ReportResult;
import com.medical.medcore.dto.response.report.ReportSection;
import com.medical.medcore.dto.response.report.TableSection;
import com.medical.medcore.report.ReportContext;
import com.medical.medcore.report.ReportProvider;
import com.medical.medcore.report.ReportQuery;
import com.medical.medcore.report.provider.BaseReportProvider;
import com.medical.medcore.report.support.ReportFormatter;
import com.medical.medcore.repository.report.ReportingPlatformRepository;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PlatformSubscriptionStatusProvider extends BaseReportProvider implements ReportProvider {

    private final ReportingPlatformRepository platform;

    @Override
    public String key() {
        return "platform-subscription-status";
    }

    @Override
    public Set<String> allowedRoles() {
        return Set.of(RoleConstants.SUPER_ADMIN);
    }

    @Override
    public ReportResult generate(ReportQuery q, ReportContext ctx) {
        LocalDate today = LocalDate.now();

        long activeCount = 0, trialCount = 0;
        for (Object[] r : platform.subscriptionStatusCounts()) {
            String status = asString(r[0]).toUpperCase();
            long count = asLong(r[1]);
            if (status.contains("ACTIVE") || status.contains("ACTIV")) {
                activeCount += count;
            }
            if (status.contains("TRIAL")) {
                trialCount += count;
            }
        }
        long expiringSoon = platform.expiringSoon(today, today.plusDays(7));
        long expired = platform.expired(today);

        List<ReportKpi> kpis = List.of(
                ReportKpi.of("Activas", ReportFormatter.number(activeCount), "check_circle", "green"),
                ReportKpi.of("En trial", ReportFormatter.number(trialCount), "schedule", "blue"),
                ReportKpi.of("Por vencer (≤7 días)", ReportFormatter.number(expiringSoon), "warning", "orange"),
                ReportKpi.of("Vencidas/morosas", ReportFormatter.number(expired), "error", "red")
        );

        String statusFilter = q.status() != null ? q.status().trim().toUpperCase() : null;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : platform.subscriptionDetail()) {
            String status = asString(r[2]);
            if (statusFilter != null && !status.toUpperCase().contains(statusFilter)) {
                continue;
            }
            String endDate = asString(r[3]);
            String daysLeft = "—";
            if (!endDate.isBlank()) {
                try {
                    long days = ChronoUnit.DAYS.between(today, LocalDate.parse(endDate));
                    daysLeft = String.valueOf(days);
                } catch (Exception ignored) {
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("org", asString(r[0]));
            row.put("plan", asString(r[1]));
            row.put("status", status);
            row.put("endDate", endDate);
            row.put("daysLeft", daysLeft);
            rows.add(row);
        }
        List<ReportSection> sections = List.of(new TableSection("Suscripciones", List.of(
                ReportColumn.text("org", "Organización"),
                ReportColumn.text("plan", "Plan"),
                ReportColumn.text("status", "Estado"),
                ReportColumn.date("endDate", "Vence el"),
                ReportColumn.text("daysLeft", "Días restantes")
        ), rows));

        return new ReportResult(key(), "Estado de Suscripciones", null, generatedAt(),
                rangeOf(q), kpis, sections);
    }
}
