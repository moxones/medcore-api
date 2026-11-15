package com.medical.medcore.schedule.repository;

import com.medical.medcore.schedule.domain.ExceptionStatus;
import com.medical.medcore.schedule.domain.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Long> {

    @Query("""
            SELECT e FROM ScheduleException e
            JOIN FETCH e.doctor d JOIN FETCH d.person
            LEFT JOIN FETCH e.branch
            WHERE e.id = :id AND e.tenantId = :tenantId
            """)
    Optional<ScheduleException> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT e FROM ScheduleException e
            LEFT JOIN FETCH e.branch
            WHERE e.doctor.id IN :doctorIds
            AND e.status = com.medical.medcore.schedule.domain.ExceptionStatus.APPROVED
            AND e.startDate <= :toDate AND e.endDate >= :fromDate
            """)
    List<ScheduleException> findApprovedInRange(@Param("doctorIds") Collection<Long> doctorIds,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT e FROM ScheduleException e
            LEFT JOIN FETCH e.branch
            WHERE e.doctor.id = :doctorId
            AND e.status IN (com.medical.medcore.schedule.domain.ExceptionStatus.APPROVED,
                             com.medical.medcore.schedule.domain.ExceptionStatus.PENDING)
            AND e.startDate <= :toDate AND e.endDate >= :fromDate
            AND (:excludeId IS NULL OR e.id <> :excludeId)
            """)
    List<ScheduleException> findOpenOverlapping(@Param("doctorId") Long doctorId,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate,
                                                @Param("excludeId") Long excludeId);

    @Query("""
            SELECT e FROM ScheduleException e
            JOIN FETCH e.doctor d JOIN FETCH d.person
            LEFT JOIN FETCH e.branch
            WHERE e.tenantId = :tenantId
            AND (:doctorId IS NULL OR d.id = :doctorId)
            AND (:status IS NULL OR e.status = :status)
            AND e.startDate <= :toDate AND e.endDate >= :fromDate
            ORDER BY e.startDate, e.startTime NULLS FIRST, e.id
            """)
    List<ScheduleException> search(@Param("tenantId") Long tenantId,
                                   @Param("doctorId") Long doctorId,
                                   @Param("status") ExceptionStatus status,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT e FROM ScheduleException e
            JOIN FETCH e.doctor d JOIN FETCH d.person
            LEFT JOIN FETCH e.branch
            WHERE e.tenantId = :tenantId
            AND e.status = com.medical.medcore.schedule.domain.ExceptionStatus.PENDING
            ORDER BY e.startDate, e.id
            """)
    List<ScheduleException> findPending(@Param("tenantId") Long tenantId);
}
