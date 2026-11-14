package com.medical.medcore.clinical.repository;

import com.medical.medcore.clinical.domain.MedicalEntryDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalEntryDiagnosisRepository extends JpaRepository<MedicalEntryDiagnosis, Long> {
    List<MedicalEntryDiagnosis> findByMedicalEntryIdOrderByIdAsc(Long medicalEntryId);
    List<MedicalEntryDiagnosis> findByMedicalEntryIdInOrderByIdAsc(List<Long> medicalEntryIds);

    void deleteByMedicalEntryId(Long medicalEntryId);
}
