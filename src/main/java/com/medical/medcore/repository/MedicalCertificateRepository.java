package com.medical.medcore.repository;

import com.medical.medcore.entity.MedicalCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalCertificateRepository extends JpaRepository<MedicalCertificate, Long> {
    List<MedicalCertificate> findByMedicalEntryIdOrderByIdAsc(Long medicalEntryId);
    List<MedicalCertificate> findByMedicalEntryIdInOrderByIdAsc(List<Long> medicalEntryIds);
}
