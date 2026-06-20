package com.medical.medcore.repository;

import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query(value = "SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.patient p LEFT JOIN FETCH p.person " +
           "LEFT JOIN FETCH a.doctor d LEFT JOIN FETCH d.person " +
           "LEFT JOIN FETCH a.branch " +
           "WHERE a.tenantId = :tenantId AND " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
           "(:patientId IS NULL OR a.patient.id = :patientId) AND " +
           "(:statusId IS NULL OR a.statusId = :statusId) AND " +
           "(:flowStatus IS NULL OR a.flowStatus = :flowStatus) AND " +
           "(:applyBranchFilter = false OR a.branch.id IN :branchIds) AND " +
           "(cast(:startDate as timestamp) IS NULL OR a.scheduledAt >= :startDate) AND " +
           "(cast(:endDate as timestamp) IS NULL OR a.scheduledAt < :endDate)",
           countQuery = "SELECT count(a) FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
           "(:patientId IS NULL OR a.patient.id = :patientId) AND " +
           "(:statusId IS NULL OR a.statusId = :statusId) AND " +
           "(:flowStatus IS NULL OR a.flowStatus = :flowStatus) AND " +
           "(:applyBranchFilter = false OR a.branch.id IN :branchIds) AND " +
           "(cast(:startDate as timestamp) IS NULL OR a.scheduledAt >= :startDate) AND " +
           "(cast(:endDate as timestamp) IS NULL OR a.scheduledAt < :endDate)")
    Page<Appointment> findByFilters(
            @Param("tenantId") Long tenantId,
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("statusId") Long statusId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("flowStatus") String flowStatus,
            @Param("applyBranchFilter") boolean applyBranchFilter,
            @Param("branchIds") List<Long> branchIds,
            Pageable pageable);

    @Query("""
            SELECT a FROM Appointment a
            LEFT JOIN FETCH a.patient p LEFT JOIN FETCH p.person
            LEFT JOIN FETCH a.doctor d LEFT JOIN FETCH d.person
            LEFT JOIN FETCH a.branch
            WHERE a.id = :id AND a.tenantId = :tenantId
            """)
    Optional<Appointment> findByIdWithDetails(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId);

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.scheduledAt >= :startDate AND a.scheduledAt < :endDate AND " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
           "(:applyBranchFilter = false OR a.branch.id IN :branchIds)")
    List<Appointment> findForCalendar(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("doctorId") Long doctorId,
            @Param("applyBranchFilter") boolean applyBranchFilter,
            @Param("branchIds") List<Long> branchIds);
            
    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.doctor.id = :doctorId AND a.scheduledAt >= :startDate AND a.scheduledAt < :endDate " +
           "AND a.statusId != :cancelledStatusId")
    List<Appointment> findByDoctorAndDate(
            @Param("tenantId") Long tenantId,
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cancelledStatusId") Long cancelledStatusId);
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.scheduledAt >= :startDate AND a.scheduledAt < :endDate")
    long countByTenantAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.statusId = :statusId AND a.scheduledAt >= :startDate AND a.scheduledAt < :endDate")
    long countByTenantAndStatusAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("statusId") Long statusId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT a.doctor.id, COUNT(a)
            FROM Appointment a
            WHERE a.tenantId = :tenantId
            AND a.doctor.id IN :doctorIds
            AND a.scheduledAt >= :from
            AND a.scheduledAt < :to
            GROUP BY a.doctor.id
            """)
    List<Object[]> countByDoctorIdInAndPeriod(
            @Param("tenantId") Long tenantId,
            @Param("doctorIds") List<Long> doctorIds,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    @Query(value = """
            SELECT d.id,
                   TRIM(COALESCE(per.first_name, '') || ' ' || COALESCE(per.last_name, '')) AS doctor_name,
                   COUNT(*) AS total,
                   SUM(CASE WHEN a.flow_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed,
                   SUM(CASE WHEN s.code ILIKE 'CANCEL%' THEN 1 ELSE 0 END) AS cancelled,
                   SUM(CASE WHEN s.code ILIKE 'NO%SHOW%' THEN 1 ELSE 0 END) AS no_show,
                   COUNT(DISTINCT a.patient_id) AS unique_patients,
                   AVG(EXTRACT(EPOCH FROM (COALESCE(a.completed_at, a.finished_at) - a.started_at)) / 60.0)
                       FILTER (WHERE a.started_at IS NOT NULL
                               AND COALESCE(a.completed_at, a.finished_at) IS NOT NULL
                               AND COALESCE(a.completed_at, a.finished_at) >= a.started_at) AS avg_minutes
            FROM appointments a
            JOIN doctors d ON d.id = a.doctor_id
            JOIN persons per ON per.id = d.person_id
            JOIN appointment_status s ON s.id = a.status_id
            WHERE a.tenant_id = :tenantId
              AND a.scheduled_at >= :startDate AND a.scheduled_at < :endDate
            GROUP BY d.id, per.first_name, per.last_name
            ORDER BY total DESC
            """, nativeQuery = true)
    List<Object[]> getProductivityByDoctor(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT ds.doctor_id, string_agg(DISTINCT sp.name, ', ' ORDER BY sp.name)
            FROM doctor_specialties ds
            JOIN specialties sp ON sp.id = ds.specialty_id
            WHERE ds.doctor_id IN (:doctorIds)
            GROUP BY ds.doctor_id
            """, nativeQuery = true)
    List<Object[]> getSpecialtiesByDoctorIds(@Param("doctorIds") List<Long> doctorIds);

    boolean existsByTenantIdAndDoctorIdAndScheduledAtAndStatusIdNot(
            Long tenantId, Long doctorId, LocalDateTime scheduledAt, Long excludedStatusId);

    /**
     * Agenda del día de un médico (citas no canceladas), con paciente y persona cargados,
     * ordenada por hora ascendente. Usada por el dashboard del médico.
     */
    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.patient p LEFT JOIN FETCH p.person " +
           "WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId AND " +
           "a.scheduledAt >= :startDate AND a.scheduledAt < :endDate AND " +
           "a.statusId != :cancelledStatusId " +
           "ORDER BY a.scheduledAt ASC")
    List<Appointment> findDoctorAgendaWithDetails(
            @Param("tenantId") Long tenantId,
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cancelledStatusId") Long cancelledStatusId);

    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.patient p LEFT JOIN FETCH p.person " +
           "WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId AND " +
           "a.flowStatus IN ('COMPLETED', 'PENDING_PAYMENT') AND a.scheduledAt < :before " +
           "ORDER BY a.scheduledAt DESC")
    List<Appointment> findDoctorCompletedBefore(
            @Param("tenantId") Long tenantId,
            @Param("doctorId") Long doctorId,
            @Param("before") LocalDateTime before,
            Pageable pageable);

    /**
     * Ids de pacientes que ya tuvieron alguna cita (no cancelada) con este médico antes
     * de una fecha. Usada para marcar `isNewPatient` en la agenda.
     */
    @Query("SELECT DISTINCT a.patient.id FROM Appointment a " +
           "WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId AND " +
           "a.scheduledAt < :before AND a.statusId != :cancelledStatusId")
    List<Long> findPatientIdsSeenByDoctorBefore(
            @Param("tenantId") Long tenantId,
            @Param("doctorId") Long doctorId,
            @Param("before") LocalDateTime before,
            @Param("cancelledStatusId") Long cancelledStatusId);

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.doctor.id IN :doctorIds AND a.scheduledAt >= :startDate AND a.scheduledAt < :endDate " +
           "AND a.statusId != :cancelledStatusId")
    List<Appointment> findByDoctorsAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("doctorIds") List<Long> doctorIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("cancelledStatusId") Long cancelledStatusId);

    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.patient p LEFT JOIN FETCH p.person " +
           "LEFT JOIN FETCH a.doctor d LEFT JOIN FETCH d.person " +
           "LEFT JOIN FETCH a.branch " +
           "WHERE a.tenantId = :tenantId AND " +
           "a.scheduledAt >= :startDate AND a.scheduledAt < :endDate AND " +
           "(:applyBranchFilter = false OR a.branch.id IN :branchIds) AND " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
           "a.statusId != :cancelledStatusId " +
           "ORDER BY a.scheduledAt ASC")
    List<Appointment> findQueue(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("applyBranchFilter") boolean applyBranchFilter,
            @Param("branchIds") List<Long> branchIds,
            @Param("doctorId") Long doctorId,
            @Param("cancelledStatusId") Long cancelledStatusId);

    @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a " +
           "WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId")
    long countDistinctPatientsByDoctor(@Param("tenantId") Long tenantId,
                                       @Param("doctorId") Long doctorId);

    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.patient p LEFT JOIN FETCH p.person " +
           "LEFT JOIN FETCH a.branch " +
           "WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId AND " +
           "a.statusId != :cancelledStatusId AND a.scheduledAt >= :from " +
           "ORDER BY a.scheduledAt ASC")
    List<Appointment> findPendingByDoctor(
            @Param("tenantId") Long tenantId,
            @Param("doctorId") Long doctorId,
            @Param("cancelledStatusId") Long cancelledStatusId,
            @Param("from") LocalDateTime from);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.doctor.id = :doctorId AND a.scheduledAt >= :startDate AND a.scheduledAt < :endDate AND " +
           "a.statusId != :cancelledStatusId")
    long countByDoctorAndDateRange(@Param("tenantId") Long tenantId,
                                   @Param("doctorId") Long doctorId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   @Param("cancelledStatusId") Long cancelledStatusId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.doctor.id = :doctorId AND a.flowStatus IN ('COMPLETED', 'PENDING_PAYMENT')")
    long countCompletedByDoctor(@Param("tenantId") Long tenantId,
                                @Param("doctorId") Long doctorId);

    @Query("SELECT a.startedAt, a.finishedAt, a.completedAt FROM Appointment a " +
           "WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId AND a.startedAt IS NOT NULL")
    List<Object[]> findConsultationTimingsByDoctor(@Param("tenantId") Long tenantId,
                                                   @Param("doctorId") Long doctorId);

    @Query(value = "SELECT DISTINCT p FROM Appointment a JOIN a.patient p JOIN p.person per " +
            "LEFT JOIN PersonDocument pd ON pd.person.id = per.id " +
            "WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId AND " +
            "(:q IS NULL OR " +
            " LOWER(CONCAT(CAST(per.firstName AS String), ' ', CAST(per.lastName AS String))) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
            " LOWER(CAST(per.firstName AS String)) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
            " LOWER(CAST(per.lastName AS String)) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
            " pd.documentNumber LIKE CONCAT('%', CAST(:q AS String), '%')) " +
            "ORDER BY p.id DESC",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Appointment a JOIN a.patient p JOIN p.person per " +
            "LEFT JOIN PersonDocument pd ON pd.person.id = per.id " +
            "WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId AND " +
            "(:q IS NULL OR " +
            " LOWER(CONCAT(CAST(per.firstName AS String), ' ', CAST(per.lastName AS String))) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
            " LOWER(CAST(per.firstName AS String)) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
            " LOWER(CAST(per.lastName AS String)) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
            " pd.documentNumber LIKE CONCAT('%', CAST(:q AS String), '%'))")
    Page<Patient> findDistinctPatientsByDoctor(@Param("tenantId") Long tenantId,
                                               @Param("doctorId") Long doctorId,
                                               @Param("q") String q,
                                               Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId AND " +
           "a.patient.id IN :patientIds AND a.statusId != :cancelledStatusId ORDER BY a.scheduledAt")
    List<Appointment> findByDoctorAndPatientIds(@Param("tenantId") Long tenantId,
                                                @Param("doctorId") Long doctorId,
                                                @Param("patientIds") List<Long> patientIds,
                                                @Param("cancelledStatusId") Long cancelledStatusId);

    @Query("SELECT a.patient.id, COUNT(a) FROM Appointment a " +
           "WHERE a.tenantId = :tenantId AND a.doctor.id = :doctorId " +
           "AND a.patient.id IN :patientIds AND a.statusId != :cancelledStatusId " +
           "GROUP BY a.patient.id")
    List<Object[]> countVisitsByPatientIds(@Param("tenantId") Long tenantId,
                                           @Param("doctorId") Long doctorId,
                                           @Param("patientIds") List<Long> patientIds,
                                           @Param("cancelledStatusId") Long cancelledStatusId);

    @Query(value = """
            SELECT DISTINCT ON (a.patient_id) a.patient_id, a.scheduled_at, a.reason
            FROM appointments a
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND a.patient_id IN (:patientIds) AND a.flow_status IN ('COMPLETED', 'PENDING_PAYMENT')
            ORDER BY a.patient_id, a.scheduled_at DESC
            """, nativeQuery = true)
    List<Object[]> findLastVisitsByPatientIds(@Param("tenantId") Long tenantId,
                                              @Param("doctorId") Long doctorId,
                                              @Param("patientIds") List<Long> patientIds);

    @Query(value = """
            SELECT DISTINCT ON (a.patient_id) a.patient_id, a.scheduled_at
            FROM appointments a
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND a.patient_id IN (:patientIds)
              AND a.scheduled_at > NOW() AND a.status_id != :cancelledStatusId
            ORDER BY a.patient_id, a.scheduled_at ASC
            """, nativeQuery = true)
    List<Object[]> findNextAppointmentsByPatientIds(@Param("tenantId") Long tenantId,
                                                    @Param("doctorId") Long doctorId,
                                                    @Param("patientIds") List<Long> patientIds,
                                                    @Param("cancelledStatusId") Long cancelledStatusId);
}
