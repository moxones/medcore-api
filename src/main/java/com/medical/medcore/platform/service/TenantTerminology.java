package com.medical.medcore.platform.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.platform.domain.BusinessType;
import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.tenancy.AfterTransaction;
import com.medical.medcore.shared.tenancy.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TenantTerminology {

    private final TenantRepository tenantRepository;
    private final Cache<Long, BusinessType> cache;

    public TenantTerminology(TenantRepository tenantRepository,
                             @Value("${app.tenancy.cache-seconds:30}") long cacheSeconds) {
        this.tenantRepository = tenantRepository;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(cacheSeconds))
                .maximumSize(10_000)
                .build();
    }

    public BusinessType current() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return BusinessType.MEDICAL;
        }
        return cache.get(tenantId, id -> TenantContext.runAsSystem(() -> tenantRepository.findById(id)
                .map(Tenant::getBusinessType)
                .orElse(BusinessType.MEDICAL)));
    }

    public String professionalName(Person person) {
        if (person == null) {
            return null;
        }
        return current().titled(person.fullName(), person.getGender());
    }

    public void evict(Long tenantId) {
        AfterTransaction.nowAndAfterCompletion(() -> cache.invalidate(tenantId));
    }
}
