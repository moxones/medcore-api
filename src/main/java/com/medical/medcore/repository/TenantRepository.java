package com.medical.medcore.repository;

import com.medical.medcore.entity.Tenant;
import com.medical.medcore.entity.enums.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySubdomain(String subdomain);
    
    boolean existsBySubdomain(String subdomain);

    Optional<Tenant> findByIdAndStatus(Long id, TenantStatus status);

    long countByStatus(TenantStatus status);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}