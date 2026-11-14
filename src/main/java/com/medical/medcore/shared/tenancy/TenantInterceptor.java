package com.medical.medcore.shared.tenancy;

import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantStatus;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.exception.TenantNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String LOCALHOST = "localhost";
    private static final String CODE_TENANT_NOT_FOUND = "TENANT_NOT_FOUND";
    private static final String CODE_TENANT_INACTIVE = "TENANT_INACTIVE";
    private static final String CODE_TENANT_MISMATCH = "TENANT_MISMATCH";
    private static final Pattern IP_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private final TenantRepository tenantRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String subdomain = resolveSubdomain(request);
        Tenant tenant = tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new TenantNotFoundException(CODE_TENANT_NOT_FOUND, "Organización no encontrada"));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new TenantNotFoundException(CODE_TENANT_INACTIVE, "La organización está inactiva");
        }
        Long tokenTenantId = TenantContext.getTenantId();
        if (tokenTenantId != null && !tokenTenantId.equals(tenant.getId())) {
            throw new ForbiddenException(CODE_TENANT_MISMATCH, "La sesión no pertenece a esta organización");
        }
        TenantContext.set(tenant.getId(), TenantContext.getCurrentUserId(), TenantContext.getBranchIds());
        MDC.put("tenantId", String.valueOf(tenant.getId()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
        MDC.remove("tenantId");
    }

    private String resolveSubdomain(HttpServletRequest request) {
        String host = request.getServerName();
        if (host == null || host.isBlank()) {
            throw new TenantNotFoundException(CODE_TENANT_NOT_FOUND, "Host inválido");
        }
        if (isDirectHost(host)) {
            String header = request.getHeader(TENANT_HEADER);
            if (header == null || header.isBlank()) {
                throw new TenantNotFoundException(CODE_TENANT_NOT_FOUND, "Organización no especificada");
            }
            return header.trim().toLowerCase();
        }
        return host.split("\\.")[0].toLowerCase();
    }

    private boolean isDirectHost(String host) {
        return LOCALHOST.equals(host) || IP_PATTERN.matcher(host).matches();
    }
}
