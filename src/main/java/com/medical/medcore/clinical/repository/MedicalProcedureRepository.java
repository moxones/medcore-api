package com.medical.medcore.clinical.repository;

import com.medical.medcore.clinical.domain.MedicalProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalProcedureRepository extends JpaRepository<MedicalProcedure, Long> {
    List<MedicalProcedure> findByMedicalEntryIdOrderByIdAsc(Long medicalEntryId);
    List<MedicalProcedure> findByMedicalEntryIdInOrderByIdAsc(List<Long> medicalEntryIds);

    void deleteByMedicalEntryId(Long medicalEntryId);
}
