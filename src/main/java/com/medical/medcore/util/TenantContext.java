package com.medical.medcore.util;

import lombok.Getter;
import lombok.Setter;

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