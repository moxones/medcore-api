package com.medical.medcore.repository;

import com.medical.medcore.entity.MedicalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalEntryRepository extends JpaRepository<MedicalEntry, Long> {

    @Query("""
            SELECT e FROM MedicalEntry e
            WHERE e.medicalRecord.id = :recordId
            ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<MedicalEntry> findByMedicalRecordId(@Param("recordId") Long recordId);

    @Query("""
            SELECT e FROM MedicalEntry e
            JOIN e.medicalRecord mr JOIN mr.patient p
            WHERE e.id = :id AND p.tenantId = :tenantId
            """)
    Optional<MedicalEntry> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT e FROM MedicalEntry e
            JOIN e.medicalRecord mr JOIN mr.patient p
            WHERE e.appointment.id = :appointmentId AND p.tenantId = :tenantId
            ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<MedicalEntry> findByAppointmentIdAndTenantId(@Param("appointmentId") Long appointmentId,
                                                      @Param("tenantId") Long tenantId);
}
