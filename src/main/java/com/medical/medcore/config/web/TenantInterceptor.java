package com.medical.medcore.config.web;

import com.medical.medcore.config.exception.TenantNotFoundException;
import com.medical.medcore.entity.Tenant;
import com.medical.medcore.entity.enums.TenantStatus;
import com.medical.medcore.repository.TenantRepository;
import com.medical.medcore.util.TenantContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantRepository tenantRepository;

    public TenantInterceptor(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String host = request.getServerName();

        if (host == null || host.isBlank()) {
            throw new TenantNotFoundException("Host inválido");
        }

        String subdomain = extractSubdomain(request, host);

        Tenant tenant = tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new TenantNotFoundException("Tenant no encontrado"));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new TenantNotFoundException("Tenant inactivo");
        }

        Long currentUserId = TenantContext.getCurrentUserId();
        Long jwtTenantId = TenantContext.getTenantId();

        if (jwtTenantId != null && !jwtTenantId.equals(tenant.getId())) {
            throw new TenantNotFoundException("Token tenant mismatch con dominio");
        }

        TenantContext.set(tenant.getId(), currentUserId);
        MDC.put("tenantId", String.valueOf(tenant.getId()));
        if (currentUserId != null) {
            MDC.put("userId", String.valueOf(currentUserId));
        }

        return true;
    }

    private String extractSubdomain(HttpServletRequest request, String host) {

        if (host.equals("localhost") || host.startsWith("127.") || host.startsWith("192.")) {
            String fallbackHeader = request.getHeader("X-Tenant-ID");
            if (fallbackHeader != null && !fallbackHeader.isBlank()) {
                return fallbackHeader;
            }
            return "default";
        }

        String[] parts = host.split("\\.");

        if (parts.length < 2) {
            throw new TenantNotFoundException("Subdominio inválido");
        }

        return parts[0];
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        TenantContext.clear();
        MDC.clear();
    }
}