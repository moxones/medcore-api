package com.medical.medcore.repository;

import com.medical.medcore.entity.MedicalProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalProcedureRepository extends JpaRepository<MedicalProcedure, Long> {
    List<MedicalProcedure> findByMedicalEntryIdOrderByIdAsc(Long medicalEntryId);
    List<MedicalProcedure> findByMedicalEntryIdInOrderByIdAsc(List<Long> medicalEntryIds);
}
