package com.medical.medcore.config.audit;

import com.medical.medcore.util.TenantContext;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorProvider")
public class AuditorProvider implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        return Optional.ofNullable(TenantContext.getCurrentUserId());
    }
}