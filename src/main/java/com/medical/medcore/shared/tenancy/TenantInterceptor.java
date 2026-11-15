package com.medical.medcore.shared.tenancy;

import com.medical.medcore.platform.service.TenantDirectory;
import com.medical.medcore.platform.service.TenantDirectory.TenantSnapshot;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.exception.PaymentRequiredException;
import com.medical.medcore.shared.exception.TenantNotFoundException;
import com.medical.medcore.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String CODE_TENANT_NOT_FOUND = "TENANT_NOT_FOUND";
    private static final String CODE_TENANT_INACTIVE = "TENANT_INACTIVE";
    private static final String CODE_TENANT_MISMATCH = "TENANT_MISMATCH";
    private static final String CODE_PLATFORM_ROLE = "PLATFORM_ROLE_OUTSIDE_PLATFORM";
    private static final String CODE_SUBSCRIPTION_EXPIRED = "SUBSCRIPTION_EXPIRED";
    private static final Set<String> PATHS_OPEN_WITHOUT_SUBSCRIPTION = Set.of("/public/tenant-info", "/auth/logout");

    private final TenantDirectory directory;
    private final TenantHostResolver hostResolver;

    public TenantInterceptor(TenantDirectory directory, TenantHostResolver hostResolver) {
        this.directory = directory;
        this.hostResolver = hostResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TenantSnapshot tenant = hostResolver.resolve(request.getServerName(), request.getHeader(TENANT_HEADER))
                .flatMap(directory::find)
                .orElseThrow(() -> new TenantNotFoundException(CODE_TENANT_NOT_FOUND, "Organización no encontrada"));
        if (!tenant.active()) {
            throw new TenantNotFoundException(CODE_TENANT_INACTIVE, "La organización está inactiva");
        }
        Long tokenTenantId = TenantContext.getTenantId();
        if (tokenTenantId != null && !tokenTenantId.equals(tenant.id())) {
            throw new ForbiddenException(CODE_TENANT_MISMATCH, "La sesión no pertenece a esta organización");
        }
        if (tenant.subscriptionBlocked() && !PATHS_OPEN_WITHOUT_SUBSCRIPTION.contains(request.getServletPath())) {
            throw new PaymentRequiredException(CODE_SUBSCRIPTION_EXPIRED,
                    "La suscripción de la organización está vencida o suspendida. Contacta a tu proveedor");
        }
        TenantContext.set(tenant.id(), TenantContext.getCurrentUserId(), TenantContext.getBranchIds());
        if (SecurityUtils.isSuperAdmin()) {
            if (!tenant.platform()) {
                throw new ForbiddenException(CODE_PLATFORM_ROLE,
                        "El rol de super administrador solo opera desde la organización de la plataforma");
            }
            TenantContext.grantCrossTenantAccess();
        }
        MDC.put("tenantId", String.valueOf(tenant.id()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
        MDC.remove("tenantId");
    }
}
