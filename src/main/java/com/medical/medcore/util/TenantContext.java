package com.medical.medcore.util;

import com.medical.medcore.config.exception.TenantNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.access.AccessDeniedException;

@Getter
@Setter
public class TenantContext {

    private static final ThreadLocal<ContextData> CURRENT = new ThreadLocal<>();

    public static void set(Long tenantId, Long userId) {
        ContextData data = new ContextData();
        data.setTenantId(tenantId);
        data.setUserId(userId);
        CURRENT.set(data);
    }

    public static Long getTenantId() {
        return CURRENT.get() != null ? CURRENT.get().getTenantId() : null;
    }

    public static Long getCurrentUserId() {
        return CURRENT.get() != null ? CURRENT.get().getUserId() : null;
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
    }
}
