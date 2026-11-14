package com.medical.medcore.shared.security;

import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.tenancy.TenantContext;

import java.util.List;

public record BranchScope(boolean restricted, List<Long> branchIds) {

    private static final List<Long> NO_MATCH_PLACEHOLDER = List.of(-1L);
    private static final BranchScope UNRESTRICTED = new BranchScope(false, NO_MATCH_PLACEHOLDER);
    private static final String DENIED = "No tienes acceso a esta sucursal";

    public static BranchScope resolve(Long requestedBranchId) {
        if (!SecurityUtils.isBranchScoped()) {
            return requestedBranchId != null ? new BranchScope(true, List.of(requestedBranchId)) : UNRESTRICTED;
        }
        List<Long> allowed = allowedBranchIds();
        if (requestedBranchId == null) {
            return new BranchScope(true, allowed);
        }
        if (!allowed.contains(requestedBranchId)) {
            throw new ForbiddenException(DENIED);
        }
        return new BranchScope(true, List.of(requestedBranchId));
    }

    public static void assertAccess(Long branchId) {
        if (!SecurityUtils.isBranchScoped()) {
            return;
        }
        if (branchId == null || !allowedBranchIds().contains(branchId)) {
            throw new ForbiddenException(DENIED);
        }
    }

    public boolean seesNothing() {
        return restricted && branchIds.isEmpty();
    }

    public List<Long> queryIds() {
        return branchIds.isEmpty() ? NO_MATCH_PLACEHOLDER : branchIds;
    }

    private static List<Long> allowedBranchIds() {
        List<Long> allowed = TenantContext.getBranchIds();
        return allowed != null ? allowed : List.of();
    }
}
