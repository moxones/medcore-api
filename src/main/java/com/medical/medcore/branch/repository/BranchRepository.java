package com.medical.medcore.branch.repository;

import com.medical.medcore.branch.domain.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Page<Branch> findByTenantId(Long tenantId, Pageable pageable);

    Page<Branch> findByTenantIdAndIsActiveTrue(Long tenantId, Pageable pageable);

    Page<Branch> findByTenantIdAndIdInAndIsActiveTrue(Long tenantId, List<Long> ids, Pageable pageable);

    Optional<Branch> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    long countByTenantIdAndIsActiveTrue(Long tenantId);

    @Query(value = """
            SELECT COUNT(*) FROM appointments a
            JOIN appointment_status s ON s.id = a.status_id
            WHERE a.branch_id = :branchId
              AND s.code = 'SCHEDULED'
              AND a.scheduled_at > NOW()
            """, nativeQuery = true)
    long countUpcomingAppointments(@Param("branchId") Long branchId);
}
