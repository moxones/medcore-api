package com.medical.medcore.repository.report;

import com.medical.medcore.entity.AppointmentReschedule;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportingRescheduleRepository extends Repository<AppointmentReschedule, Long> {

    @Query(value = """
            SELECT COUNT(*)
            FROM appointment_reschedules r
            JOIN appointments a ON a.id = r.appointment_id
            WHERE a.tenant_id = :tenantId
              AND r.created_at >= :from AND r.created_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            """, nativeQuery = true)
    long countInRange(@Param("tenantId") Long tenantId,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to,
                      @Param("applyBranch") boolean applyBranch,
                      @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT COALESCE(NULLIF(TRIM(r.reason), ''), 'Sin motivo'), COUNT(*)
            FROM appointment_reschedules r
            JOIN appointments a ON a.id = r.appointment_id
            WHERE a.tenant_id = :tenantId
              AND r.created_at >= :from AND r.created_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY 1 ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> reasonBreakdown(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("applyBranch") boolean applyBranch,
                                   @Param("branchIds") List<Long> branchIds);
}
