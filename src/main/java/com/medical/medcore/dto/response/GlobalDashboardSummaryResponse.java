package com.medical.medcore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalDashboardSummaryResponse {
    private long totalTenants;
    private long activeTenants;
    private long inactiveTenants;
    private long tenantsCreatedThisMonth;
    private long tenantsCreatedLastMonth;
    private long activeSubscriptions;
    private long expiredSubscriptions;
    private long totalUsers;
    private long totalAllowedUsers;
}
