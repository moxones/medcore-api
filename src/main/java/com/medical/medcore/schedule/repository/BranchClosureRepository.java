package com.medical.medcore.schedule.repository;

import com.medical.medcore.schedule.domain.BranchClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BranchClosureRepository extends JpaRepository<BranchClosure, Long> {

    @Query("""
            SELECT c FROM BranchClosure c
            LEFT JOIN FETCH c.branch
            WHERE c.tenantId = :tenantId
            AND c.closureDate >= :fromDate AND c.closureDate <= :toDate
            AND (:branchId IS NULL OR c.branch IS NULL OR c.branch.id = :branchId)
            ORDER BY c.closureDate, c.id
            """)
    List<BranchClosure> findInRange(@Param("tenantId") Long tenantId,
                                    @Param("branchId") Long branchId,
                                    @Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT c FROM BranchClosure c
            LEFT JOIN FETCH c.branch
            WHERE c.id = :id AND c.tenantId = :tenantId
            """)
    Optional<BranchClosure> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT COUNT(c) > 0 FROM BranchClosure c
            WHERE c.tenantId = :tenantId AND c.closureDate = :date
            AND ((:branchId IS NULL AND c.branch IS NULL) OR c.branch.id = :branchId)
            """)
    boolean existsSameScope(@Param("tenantId") Long tenantId,
                            @Param("branchId") Long branchId,
                            @Param("date") LocalDate date);
}
