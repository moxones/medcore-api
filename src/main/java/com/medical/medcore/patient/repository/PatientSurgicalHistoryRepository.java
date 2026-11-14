package com.medical.medcore.patient.repository;

import com.medical.medcore.patient.domain.PatientSurgicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientSurgicalHistoryRepository extends JpaRepository<PatientSurgicalHistory, Long> {
    List<PatientSurgicalHistory> findByPatientIdOrderByIdDesc(Long patientId);
}
