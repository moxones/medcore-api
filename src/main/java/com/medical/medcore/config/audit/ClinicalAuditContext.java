package com.medical.medcore.config.audit;

import com.medical.medcore.util.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class ClinicalAuditContext {

    @PersistenceContext
    private EntityManager entityManager;

    public void apply() {
        Long userId = TenantContext.getCurrentUserId();
        Long tenantId = TenantContext.getTenantId();

        entityManager.createNativeQuery("SELECT set_config('app.current_user_id', :userId, true)")
                .setParameter("userId", userId != null ? userId.toString() : "")
                .getSingleResult();

        entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tenantId, true)")
                .setParameter("tenantId", tenantId != null ? tenantId.toString() : "")
                .getSingleResult();
    }
}
