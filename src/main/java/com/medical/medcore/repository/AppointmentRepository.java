package com.medical.medcore.repository;

import com.medical.medcore.entity.Appointment;
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

    @Query("SELECT new com.medical.medcore.dto.response.DoctorProductivityResponse(" +
           "a.doctor.id, 'Doctor ' || a.doctor.id, COUNT(a)) " +
           "FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.scheduledAt >= :startDate AND a.scheduledAt < :endDate " +
           "GROUP BY a.doctor.id")
    java.util.List<com.medical.medcore.dto.response.DoctorProductivityResponse> getProductivityByDoctor(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    boolean existsByTenantIdAndDoctorIdAndScheduledAtAndStatusIdNot(
            Long tenantId, Long doctorId, LocalDateTime scheduledAt, Long excludedStatusId);

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
}
