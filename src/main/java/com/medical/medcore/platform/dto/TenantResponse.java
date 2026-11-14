package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantStatus;

import java.time.LocalDateTime;

public record TenantResponse(
        Long id,
        String subdomain,
        String name,
        TenantStatus status,
        String logoUrl,
        String primaryColor,
        String subtitle,
        String contactEmail,
        String contactPhone,
        String address,
        String taxId,
        String timezone,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(), tenant.getSubdomain(), tenant.getName(), tenant.getStatus(), tenant.getLogoUrl(),
                tenant.getPrimaryColor(), tenant.getSubtitle(), tenant.getContactEmail(), tenant.getContactPhone(),
                tenant.getAddress(), tenant.getTaxId(), tenant.getTimezone(), tenant.getCurrency(),
                tenant.getCreatedAt(), tenant.getUpdatedAt());
    }
}
