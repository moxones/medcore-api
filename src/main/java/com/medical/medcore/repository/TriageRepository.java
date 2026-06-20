package com.medical.medcore.repository;

import com.medical.medcore.entity.Triage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TriageRepository extends JpaRepository<Triage, Long> {

    List<Triage> findByAppointmentIdOrderByMeasuredAtDescIdDesc(Long appointmentId);

    Optional<Triage> findFirstByAppointmentIdOrderByMeasuredAtDescIdDesc(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    @Query("SELECT DISTINCT t.appointment.id FROM Triage t WHERE t.appointment.id IN :appointmentIds")
    List<Long> findAppointmentIdsWithTriage(@Param("appointmentIds") List<Long> appointmentIds);

    /**
     * Triajes cuyas citas caen dentro de un rango de fechas, con cita, paciente,
     * persona y médico cargados. Filtra por médico de forma opcional. Usado por la
     * vista "Hoy" del médico (`GET /triage/today`).
     */
    @Query("SELECT t FROM Triage t " +
           "JOIN FETCH t.appointment a " +
           "LEFT JOIN FETCH a.patient p LEFT JOIN FETCH p.person " +
           "LEFT JOIN FETCH a.doctor d LEFT JOIN FETCH d.person " +
           "WHERE a.tenantId = :tenantId AND " +
           "a.scheduledAt >= :startDate AND a.scheduledAt < :endDate AND " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) " +
           "ORDER BY a.scheduledAt ASC, t.createdAt DESC")
    List<Triage> findDaySummary(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("doctorId") Long doctorId);
}
