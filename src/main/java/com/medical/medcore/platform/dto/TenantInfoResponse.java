package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.BusinessType;
import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.platform.domain.Tenant;

import java.util.List;
import java.util.Set;

public record TenantInfoResponse(
        String name,
        String logoUrl,
        String subtitle,
        String timezone,
        String currency,
        BusinessType businessType,
        ProfessionalTermsResponse terms,
        TenantThemeResponse theme,
        List<ClinicModule> modules,
        boolean subscriptionExpired
) {

    public static TenantInfoResponse from(Tenant tenant, Set<ClinicModule> modules, boolean subscriptionExpired) {
        return new TenantInfoResponse(tenant.getName(), tenant.getLogoUrl(), tenant.getSubtitle(),
                tenant.getTimezone(), tenant.getCurrency(), tenant.getBusinessType(),
                ProfessionalTermsResponse.from(tenant.getBusinessType()), TenantThemeResponse.from(tenant), modules.stream().sorted().toList(),
                subscriptionExpired);
    }
}
