package com.medical.medcore.repository;

import com.medical.medcore.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByPatientId(Long patientId);

    @Query("SELECT mr FROM MedicalRecord mr JOIN mr.patient p WHERE mr.id = :id AND p.tenantId = :tenantId")
    Optional<MedicalRecord> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT mr FROM MedicalRecord mr JOIN mr.patient p WHERE p.id = :patientId AND p.tenantId = :tenantId")
    Optional<MedicalRecord> findByPatientIdAndTenantId(@Param("patientId") Long patientId, @Param("tenantId") Long tenantId);
}
