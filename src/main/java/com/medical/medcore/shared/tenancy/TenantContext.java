package com.medical.medcore.shared.tenancy;

import com.medical.medcore.shared.exception.TenantNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.function.Supplier;

public final class TenantContext {

    private static final ThreadLocal<ContextData> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId, Long userId) {
        set(tenantId, userId, null);
    }

    public static void set(Long tenantId, Long userId, List<Long> branchIds) {
        ContextData previous = CURRENT.get();
        boolean crossTenant = previous != null && previous.crossTenant();
        CURRENT.set(new ContextData(tenantId, userId, branchIds, crossTenant));
    }

    public static Long getTenantId() {
        return CURRENT.get() != null ? CURRENT.get().tenantId() : null;
    }

    public static Long getCurrentUserId() {
        return CURRENT.get() != null ? CURRENT.get().userId() : null;
    }

    public static List<Long> getBranchIds() {
        return CURRENT.get() != null ? CURRENT.get().branchIds() : null;
    }

    public static boolean isCrossTenant() {
        return CURRENT.get() != null && CURRENT.get().crossTenant();
    }

    public static void grantCrossTenantAccess() {
        ContextData current = CURRENT.get();
        CURRENT.set(current == null
                ? new ContextData(null, null, null, true)
                : new ContextData(current.tenantId(), current.userId(), current.branchIds(), true));
    }

    public static <T> T runAsSystem(Supplier<T> work) {
        ContextData previous = CURRENT.get();
        CURRENT.set(new ContextData(
                previous != null ? previous.tenantId() : null,
                previous != null ? previous.userId() : null,
                previous != null ? previous.branchIds() : null,
                true));
        try {
            return work.get();
        } finally {
            restore(previous);
        }
    }

    public static void runAsSystem(Runnable work) {
        runAsSystem(() -> {
            work.run();
            return null;
        });
    }

    public static Long requireTenantId() {
        Long tenantId = getTenantId();
        if (tenantId == null) {
            throw new TenantNotFoundException("Tenant no resuelto");
        }
        return tenantId;
    }

    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new AccessDeniedException("Usuario autenticado no resuelto");
        }
        return userId;
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static void restore(ContextData previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    private record ContextData(Long tenantId, Long userId, List<Long> branchIds, boolean crossTenant) {
    }
}
