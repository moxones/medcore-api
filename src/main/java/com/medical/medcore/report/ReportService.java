package com.medical.medcore.report;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.response.report.ReportResult;
import com.medical.medcore.report.export.ReportExporter;
import com.medical.medcore.util.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final Map<String, ReportProvider> providers;
    private final Map<ReportFormat, ReportExporter> exporters;

    public ReportService(List<ReportProvider> providerList, List<ReportExporter> exporterList) {
        this.providers = providerList.stream()
                .collect(Collectors.toUnmodifiableMap(ReportProvider::key, Function.identity()));
        this.exporters = exporterList.stream()
                .collect(Collectors.toUnmodifiableMap(ReportExporter::format, Function.identity()));
    }

    public ReportResult getReport(String key, ReportQuery rawQuery) {
        ReportProvider provider = resolveProvider(key);
        ReportContext ctx = resolveContext();
        authorize(provider, ctx);
        ReportQuery query = normalize(rawQuery);
        return provider.generate(query, ctx);
    }

    public byte[] export(String key, ReportFormat format, ReportQuery rawQuery) {
        ReportExporter exporter = exporters.get(format);
        if (exporter == null) {
            throw new BadRequestException("Formato de exportación no soportado: " + format);
        }
        ReportResult result = getReport(key, rawQuery);
        return exporter.export(result);
    }

    private ReportProvider resolveProvider(String key) {
        ReportProvider provider = providers.get(key);
        if (provider == null) {
            throw new NotFoundException("Reporte desconocido: " + key);
        }
        return provider;
    }

    private void authorize(ReportProvider provider, ReportContext ctx) {
        boolean allowed = provider.allowedRoles().stream().anyMatch(ctx::hasRole);
        if (!allowed) {
            throw new AccessDeniedException("No tiene acceso a este reporte");
        }
    }

    private ReportContext resolveContext() {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getCurrentUserId();
        List<Long> branchIds = TenantContext.getBranchIds();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = auth == null ? Set.of() : auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());

        return new ReportContext(tenantId, userId, roles, branchIds);
    }

    private ReportQuery normalize(ReportQuery q) {
        LocalDate from = q.from();
        LocalDate to = q.to();
        LocalDate today = LocalDate.now();

        if (from == null && to == null) {
            from = today.withDayOfMonth(1);
            to = today;
        } else if (from == null) {
            from = to.withDayOfMonth(1);
        } else if (to == null) {
            to = from.plusMonths(1).withDayOfMonth(1).minusDays(1);
        }

        if (from.isAfter(to)) {
            throw new BadRequestException("Rango de fechas inválido: 'from' es posterior a 'to'");
        }

        return new ReportQuery(from, to, q.branchId(), q.doctorId(), q.specialtyId(), q.status());
    }
}
