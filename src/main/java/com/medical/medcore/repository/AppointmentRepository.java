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

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
           "(:statusId IS NULL OR a.statusId = :statusId) AND " +
           "(cast(:startDate as timestamp) IS NULL OR a.scheduledAt >= :startDate) AND " +
           "(cast(:endDate as timestamp) IS NULL OR a.scheduledAt < :endDate)")
    Page<Appointment> findByFilters(
            @Param("tenantId") Long tenantId,
            @Param("doctorId") Long doctorId,
            @Param("statusId") Long statusId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.scheduledAt >= :startDate AND a.scheduledAt < :endDate AND " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
           "(:branchId IS NULL OR a.branch.id = :branchId)")
    List<Appointment> findForCalendar(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("doctorId") Long doctorId,
            @Param("branchId") Long branchId);
            
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

    @Query("SELECT new com.medical.medcore.dto.response.DoctorProductivityResponse(" +
           "a.doctor.id, 'Doctor ' || a.doctor.id, COUNT(a)) " +
           "FROM Appointment a WHERE a.tenantId = :tenantId AND " +
           "a.scheduledAt >= :startDate AND a.scheduledAt < :endDate " +
           "GROUP BY a.doctor.id")
    java.util.List<com.medical.medcore.dto.response.DoctorProductivityResponse> getProductivityByDoctor(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
