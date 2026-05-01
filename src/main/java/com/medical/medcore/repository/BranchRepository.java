package com.medical.medcore.repository;

import com.medical.medcore.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Page<Branch> findByTenantIdAndIsActiveTrue(Long tenantId, Pageable pageable);
    Optional<Branch> findByIdAndTenantId(Long id, Long tenantId);
    long countByTenantIdAndIsActiveTrue(Long tenantId);
}
