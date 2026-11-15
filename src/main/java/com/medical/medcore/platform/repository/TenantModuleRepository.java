package com.medical.medcore.platform.repository;

import com.medical.medcore.platform.domain.TenantModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TenantModuleRepository extends JpaRepository<TenantModule, Long> {

    List<TenantModule> findByTenantId(Long tenantId);

    List<TenantModule> findByTenantIdIn(Collection<Long> tenantIds);
}
