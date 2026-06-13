package com.medical.medcore.repository;

import com.medical.medcore.entity.TenantProcessConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantProcessConfigRepository extends JpaRepository<TenantProcessConfig, Long> {

    List<TenantProcessConfig> findByTenantId(Long tenantId);

    Optional<TenantProcessConfig> findByTenantIdAndProcess(Long tenantId, String process);
}
