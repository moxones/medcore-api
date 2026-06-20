package com.medical.medcore.repository;

import com.medical.medcore.entity.MedicalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT COUNT(e) FROM MedicalEntry e
            JOIN e.medicalRecord mr JOIN mr.patient p
            WHERE p.tenantId = :tenantId AND e.createdBy = :userId AND e.signedAt IS NULL
            """)
    long countUnsignedByCreator(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Query(value = "SELECT DISTINCT e FROM MedicalEntry e " +
            "JOIN e.appointment ap JOIN e.medicalRecord mr JOIN mr.patient p JOIN p.person per " +
            "WHERE p.tenantId = :tenantId AND ap.doctor.id = :doctorId AND " +
            "EXISTS (SELECT 1 FROM Prescription pr WHERE pr.medicalEntry = e) AND " +
            "(:q IS NULL OR " +
            " LOWER(CONCAT(CAST(per.firstName AS String), ' ', CAST(per.lastName AS String))) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
            " EXISTS (SELECT 1 FROM Prescription pr2 WHERE pr2.medicalEntry = e AND " +
            "         LOWER(pr2.medication) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')))) " +
            "ORDER BY e.id DESC",
            countQuery = "SELECT COUNT(DISTINCT e) FROM MedicalEntry e " +
            "JOIN e.appointment ap JOIN e.medicalRecord mr JOIN mr.patient p JOIN p.person per " +
            "WHERE p.tenantId = :tenantId AND ap.doctor.id = :doctorId AND " +
            "EXISTS (SELECT 1 FROM Prescription pr WHERE pr.medicalEntry = e) AND " +
            "(:q IS NULL OR " +
            " LOWER(CONCAT(CAST(per.firstName AS String), ' ', CAST(per.lastName AS String))) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')) OR " +
            " EXISTS (SELECT 1 FROM Prescription pr2 WHERE pr2.medicalEntry = e AND " +
            "         LOWER(pr2.medication) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))))")
    Page<MedicalEntry> findPrescriptionEntriesByDoctor(@Param("tenantId") Long tenantId,
                                                       @Param("doctorId") Long doctorId,
                                                       @Param("q") String q,
                                                       Pageable pageable);
}
