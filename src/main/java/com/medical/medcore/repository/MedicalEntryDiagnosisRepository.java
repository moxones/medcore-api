package com.medical.medcore.repository;

import com.medical.medcore.entity.MedicalEntryDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalEntryDiagnosisRepository extends JpaRepository<MedicalEntryDiagnosis, Long> {
    List<MedicalEntryDiagnosis> findByMedicalEntryIdOrderByIdAsc(Long medicalEntryId);
    List<MedicalEntryDiagnosis> findByMedicalEntryIdInOrderByIdAsc(List<Long> medicalEntryIds);
}
