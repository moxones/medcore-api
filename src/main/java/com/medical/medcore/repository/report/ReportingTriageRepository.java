package com.medical.medcore.repository.report;

import com.medical.medcore.entity.Triage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportingTriageRepository extends Repository<Triage, Long> {

    @Query(value = """
            SELECT COALESCE(t.priority_level, 'SIN_PRIORIDAD'), COUNT(*)
            FROM triage t
            JOIN appointments a ON a.id = t.appointment_id
            WHERE a.tenant_id = :tenantId
              AND t.created_at >= :from AND t.created_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY t.priority_level
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> byPriority(@Param("tenantId") Long tenantId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              @Param("applyBranch") boolean applyBranch,
                              @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT to_char(t.created_at, 'YYYY-MM-DD') AS day,
                   COUNT(*) AS total,
                   SUM(CASE WHEN upper(t.priority_level) IN ('ALTA','HIGH','URGENTE','URGENT') THEN 1 ELSE 0 END) AS high,
                   SUM(CASE WHEN upper(t.priority_level) IN ('MEDIA','MEDIUM','NORMAL') THEN 1 ELSE 0 END) AS medium,
                   SUM(CASE WHEN upper(t.priority_level) IN ('BAJA','LOW') THEN 1 ELSE 0 END) AS low
            FROM triage t
            JOIN appointments a ON a.id = t.appointment_id
            WHERE a.tenant_id = :tenantId
              AND t.created_at >= :from AND t.created_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            GROUP BY day ORDER BY day
            """, nativeQuery = true)
    List<Object[]> dailyByPriority(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("applyBranch") boolean applyBranch,
                                   @Param("branchIds") List<Long> branchIds);

    @Query(value = """
            SELECT COUNT(*) AS total,
                   COUNT(DISTINCT a.patient_id) AS patients,
                   SUM(CASE WHEN upper(t.priority_level) IN ('ALTA','HIGH','URGENTE','URGENT') THEN 1 ELSE 0 END) AS high
            FROM triage t
            JOIN appointments a ON a.id = t.appointment_id
            WHERE a.tenant_id = :tenantId
              AND t.created_at >= :from AND t.created_at < :to
              AND (:applyBranch = false OR a.branch_id IN (:branchIds))
            """, nativeQuery = true)
    List<Object[]> summary(@Param("tenantId") Long tenantId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to,
                           @Param("applyBranch") boolean applyBranch,
                           @Param("branchIds") List<Long> branchIds);
}
