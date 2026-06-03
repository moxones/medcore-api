package com.medical.medcore.repository;

import com.medical.medcore.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByMedicalEntryIdIn(List<Long> medicalEntryIds);

    List<Prescription> findByMedicalEntryId(Long medicalEntryId);
}
