package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.BusinessType;
import com.medical.medcore.platform.domain.Tenant;

public record OrganizationProfileResponse(
        String subdomain,
        String customDomain,
        String name,
        String subtitle,
        String contactEmail,
        String contactPhone,
        String address,
        String taxId,
        String logoUrl,
        BusinessType businessType,
        TenantThemeResponse theme
) {

    public static OrganizationProfileResponse from(Tenant tenant) {
        return new OrganizationProfileResponse(tenant.getSubdomain(), tenant.getCustomDomain(), tenant.getName(),
                tenant.getSubtitle(), tenant.getContactEmail(), tenant.getContactPhone(), tenant.getAddress(),
                tenant.getTaxId(), tenant.getLogoUrl(), tenant.getBusinessType(), TenantThemeResponse.from(tenant));
    }
}
