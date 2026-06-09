package com.medical.medcore.repository.report;

import com.medical.medcore.entity.Appointment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Agregaciones para los reportes del paciente autenticado (alcance por patient_id). */
public interface ReportingPatientRepository extends Repository<Appointment, Long> {

    /** [date, doctorName, specialty, statusName] historial de citas del paciente. */
    @Query(value = """
            SELECT to_char(a.scheduled_at, 'YYYY-MM-DD'),
                   TRIM(COALESCE(dper.first_name, '') || ' ' || COALESCE(dper.last_name, '')) AS doctor,
                   (SELECT sp.name FROM doctor_specialties ds
                      JOIN specialties sp ON sp.id = ds.specialty_id
                      WHERE ds.doctor_id = a.doctor_id LIMIT 1) AS specialty,
                   s.name AS status
            FROM appointments a
            JOIN doctors d ON d.id = a.doctor_id
            JOIN persons dper ON dper.id = d.person_id
            JOIN appointment_status s ON s.id = a.status_id
            WHERE a.tenant_id = :tenantId AND a.patient_id = :patientId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
            ORDER BY a.scheduled_at DESC
            """, nativeQuery = true)
    List<Object[]> appointmentHistory(@Param("tenantId") Long tenantId,
                                      @Param("patientId") Long patientId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /** [total, attended, cancelled, distinctDoctors] KPIs del historial del paciente. */
    @Query(value = """
            SELECT COUNT(*),
                   SUM(CASE WHEN a.flow_status = 'COMPLETED' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN s.code ILIKE 'CANCEL%' THEN 1 ELSE 0 END),
                   COUNT(DISTINCT a.doctor_id)
            FROM appointments a
            JOIN appointment_status s ON s.id = a.status_id
            WHERE a.tenant_id = :tenantId AND a.patient_id = :patientId
              AND a.scheduled_at >= :from AND a.scheduled_at < :to
            """, nativeQuery = true)
    List<Object[]> appointmentSummary(@Param("tenantId") Long tenantId,
                                      @Param("patientId") Long patientId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /** [medication, dosage, instructions, date, isActive] recetas del paciente. */
    @Query(value = """
            SELECT pr.medication, pr.dosage, pr.instructions,
                   to_char(pr.created_at, 'YYYY-MM-DD'), pr.is_active
            FROM prescriptions pr
            JOIN medical_entries me ON me.id = pr.medical_entry_id
            JOIN appointments a ON a.id = me.appointment_id
            WHERE a.tenant_id = :tenantId AND a.patient_id = :patientId
              AND pr.created_at >= :from AND pr.created_at < :to
            ORDER BY pr.created_at DESC
            """, nativeQuery = true)
    List<Object[]> prescriptions(@Param("tenantId") Long tenantId,
                                 @Param("patientId") Long patientId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    /** [date, weight, bmi, blood_pressure, heart_rate] mediciones de signos vitales del paciente. */
    @Query(value = """
            SELECT to_char(COALESCE(t.measured_at, t.created_at), 'YYYY-MM-DD'),
                   t.weight, t.bmi, t.blood_pressure, t.heart_rate, t.temperature
            FROM triage t
            JOIN appointments a ON a.id = t.appointment_id
            WHERE a.tenant_id = :tenantId AND a.patient_id = :patientId
              AND COALESCE(t.measured_at, t.created_at) >= :from
              AND COALESCE(t.measured_at, t.created_at) < :to
            ORDER BY COALESCE(t.measured_at, t.created_at) ASC
            """, nativeQuery = true)
    List<Object[]> vitalsHistory(@Param("tenantId") Long tenantId,
                                 @Param("patientId") Long patientId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);
}
