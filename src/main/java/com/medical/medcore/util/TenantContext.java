package com.medical.medcore.util;

import com.medical.medcore.config.exception.TenantNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

@Getter
@Setter
public class TenantContext {

    private static final ThreadLocal<ContextData> CURRENT = new ThreadLocal<>();

    public static void set(Long tenantId, Long userId) {
        set(tenantId, userId, null);
    }

    public static void set(Long tenantId, Long userId, List<Long> branchIds) {
        ContextData data = new ContextData();
        data.setTenantId(tenantId);
        data.setUserId(userId);
        data.setBranchIds(branchIds);
        CURRENT.set(data);
    }

    public static Long getTenantId() {
        return CURRENT.get() != null ? CURRENT.get().getTenantId() : null;
    }

    public static Long getCurrentUserId() {
        return CURRENT.get() != null ? CURRENT.get().getUserId() : null;
    }

    /** Sucursales asignadas al usuario autenticado (personal operativo). null = no resuelto. */
    public static List<Long> getBranchIds() {
        return CURRENT.get() != null ? CURRENT.get().getBranchIds() : null;
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

    @Getter
    @Setter
    private static class ContextData {
        private Long tenantId;
        private Long userId;
        private List<Long> branchIds;
    }
}
