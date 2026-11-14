package com.medical.medcore.patient.repository;

import com.medical.medcore.patient.domain.PatientFamilyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientFamilyHistoryRepository extends JpaRepository<PatientFamilyHistory, Long> {
    List<PatientFamilyHistory> findByPatientIdOrderByIdDesc(Long patientId);
}
