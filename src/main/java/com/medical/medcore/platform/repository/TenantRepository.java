package com.medical.medcore.platform.repository;

import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySubdomain(String subdomain);
    
    boolean existsBySubdomain(String subdomain);

    Optional<Tenant> findByCustomDomain(String customDomain);

    boolean existsByCustomDomainAndIdNot(String customDomain, Long id);

    Optional<Tenant> findByIdAndStatus(Long id, TenantStatus status);

    long countByStatus(TenantStatus status);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
