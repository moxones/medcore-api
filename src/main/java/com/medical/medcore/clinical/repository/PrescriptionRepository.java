package com.medical.medcore.clinical.repository;

import com.medical.medcore.clinical.domain.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByMedicalEntryIdIn(List<Long> medicalEntryIds);

    List<Prescription> findByMedicalEntryId(Long medicalEntryId);

    void deleteByMedicalEntryId(Long medicalEntryId);
}
