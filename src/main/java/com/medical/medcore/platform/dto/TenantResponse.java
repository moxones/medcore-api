package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.BusinessType;
import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record TenantResponse(
        Long id,
        String subdomain,
        String customDomain,
        String name,
        TenantStatus status,
        String logoUrl,
        String subtitle,
        String contactEmail,
        String contactPhone,
        String address,
        String taxId,
        String timezone,
        String currency,
        BusinessType businessType,
        TenantThemeResponse theme,
        List<ClinicModule> modules,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TenantResponse from(Tenant tenant, Set<ClinicModule> modules) {
        return new TenantResponse(
                tenant.getId(), tenant.getSubdomain(), tenant.getCustomDomain(), tenant.getName(), tenant.getStatus(), tenant.getLogoUrl(),
                tenant.getSubtitle(), tenant.getContactEmail(), tenant.getContactPhone(), tenant.getAddress(),
                tenant.getTaxId(), tenant.getTimezone(), tenant.getCurrency(), tenant.getBusinessType(),
                TenantThemeResponse.from(tenant), modules.stream().sorted().toList(), tenant.getCreatedAt(), tenant.getUpdatedAt());
    }
}
