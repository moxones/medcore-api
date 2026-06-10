package com.medical.medcore.report;

import java.util.List;
import java.util.Set;

public record ReportContext(
        Long tenantId,
        Long userId,
        Set<String> roles,
        List<Long> branchIds
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasBranchScope() {
        return branchIds != null && !branchIds.isEmpty();
    }
}
