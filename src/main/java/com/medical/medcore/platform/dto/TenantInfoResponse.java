package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.Tenant;

public record TenantInfoResponse(
        String name,
        String logoUrl,
        String primaryColor,
        String subtitle,
        String timezone,
        String currency
) {

    public static TenantInfoResponse from(Tenant tenant) {
        return new TenantInfoResponse(tenant.getName(), tenant.getLogoUrl(), tenant.getPrimaryColor(),
                tenant.getSubtitle(), tenant.getTimezone(), tenant.getCurrency());
    }
}
