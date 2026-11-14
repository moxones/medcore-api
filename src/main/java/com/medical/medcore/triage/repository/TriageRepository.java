package com.medical.medcore.triage.repository;

import com.medical.medcore.triage.domain.Triage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TriageRepository extends JpaRepository<Triage, Long> {

    List<Triage> findByAppointmentIdOrderByMeasuredAtDescIdDesc(Long appointmentId);

    Optional<Triage> findFirstByAppointmentIdOrderByMeasuredAtDescIdDesc(Long appointmentId);

    Optional<Triage> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByAppointmentId(Long appointmentId);

    @Query("SELECT DISTINCT t.appointment.id FROM Triage t WHERE t.appointment.id IN :appointmentIds")
    List<Long> findAppointmentIdsWithTriage(@Param("appointmentIds") List<Long> appointmentIds);

    @Query("""
            SELECT t FROM Triage t
            JOIN FETCH t.appointment a
            JOIN FETCH a.patient p JOIN FETCH p.person
            JOIN FETCH a.doctor d JOIN FETCH d.person
            WHERE t.tenantId = :tenantId
              AND a.scheduledAt >= :startDate AND a.scheduledAt < :endDate
              AND (:doctorId IS NULL OR d.id = :doctorId)
              AND (:applyBranchFilter = false OR a.branch.id IN :branchIds)
            ORDER BY a.scheduledAt ASC, t.id DESC
            """)
    List<Triage> findDaySummary(@Param("tenantId") Long tenantId,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate,
                                @Param("doctorId") Long doctorId,
                                @Param("applyBranchFilter") boolean applyBranchFilter,
                                @Param("branchIds") List<Long> branchIds);

    @Query("""
            SELECT t FROM Triage t
            JOIN FETCH t.appointment a
            JOIN FETCH a.patient p JOIN FETCH p.person
            JOIN FETCH a.doctor d JOIN FETCH d.person
            WHERE t.tenantId = :tenantId AND p.id = :patientId
            ORDER BY t.id DESC
            """)
    List<Triage> findByPatient(@Param("tenantId") Long tenantId, @Param("patientId") Long patientId, Pageable pageable);
}
