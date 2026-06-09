package com.medical.medcore.repository.report;

import com.medical.medcore.entity.MedicalEntryDiagnosis;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agregaciones clínicas (diagnósticos, recetas, órdenes) para reportería. El alcance del médico
 * se resuelve por la cita asociada a la entrada clínica ({@code appointments.doctor_id}).
 */
public interface ReportingClinicalRepository extends Repository<MedicalEntryDiagnosis, Long> {

    /** [code, description, count] top diagnósticos CIE-10 de un médico. */
    @Query(value = """
            SELECT COALESCE(c.code, '—'), COALESCE(c.description, md.description), COUNT(*)
            FROM medical_entry_diagnoses md
            JOIN medical_entries me ON me.id = md.medical_entry_id
            JOIN appointments a ON a.id = me.appointment_id
            LEFT JOIN cie10_codes c ON c.id = md.cie10_id
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND md.created_at >= :from AND md.created_at < :to
            GROUP BY c.code, c.description, md.description
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> topDiagnosesForDoctor(@Param("tenantId") Long tenantId,
                                         @Param("doctorId") Long doctorId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /** [total, distinctCie10] KPIs de diagnósticos de un médico. */
    @Query(value = """
            SELECT COUNT(*), COUNT(DISTINCT md.cie10_id)
            FROM medical_entry_diagnoses md
            JOIN medical_entries me ON me.id = md.medical_entry_id
            JOIN appointments a ON a.id = me.appointment_id
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND md.created_at >= :from AND md.created_at < :to
            """, nativeQuery = true)
    List<Object[]> diagnosisSummaryForDoctor(@Param("tenantId") Long tenantId,
                                             @Param("doctorId") Long doctorId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    /** [medication, count] medicamentos más prescritos por un médico. */
    @Query(value = """
            SELECT pr.medication, COUNT(*)
            FROM prescriptions pr
            JOIN medical_entries me ON me.id = pr.medical_entry_id
            JOIN appointments a ON a.id = me.appointment_id
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND pr.created_at >= :from AND pr.created_at < :to
            GROUP BY pr.medication ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> topMedicationsForDoctor(@Param("tenantId") Long tenantId,
                                           @Param("doctorId") Long doctorId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    /** [totalPrescriptions, distinctMedications] KPIs de recetas de un médico. */
    @Query(value = """
            SELECT COUNT(*), COUNT(DISTINCT pr.medication)
            FROM prescriptions pr
            JOIN medical_entries me ON me.id = pr.medical_entry_id
            JOIN appointments a ON a.id = me.appointment_id
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND pr.created_at >= :from AND pr.created_at < :to
            """, nativeQuery = true)
    List<Object[]> prescriptionSummaryForDoctor(@Param("tenantId") Long tenantId,
                                                @Param("doctorId") Long doctorId,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    /** [orderType, count] órdenes médicas por tipo de un médico. */
    @Query(value = """
            SELECT mo.order_type, COUNT(*)
            FROM medical_orders mo
            JOIN medical_entries me ON me.id = mo.medical_entry_id
            JOIN appointments a ON a.id = me.appointment_id
            WHERE a.tenant_id = :tenantId AND a.doctor_id = :doctorId
              AND mo.requested_at >= :from AND mo.requested_at < :to
            GROUP BY mo.order_type ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> ordersByTypeForDoctor(@Param("tenantId") Long tenantId,
                                         @Param("doctorId") Long doctorId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}
