package com.medical.medcore.repository;

import com.medical.medcore.entity.PatientSurgicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientSurgicalHistoryRepository extends JpaRepository<PatientSurgicalHistory, Long> {
    List<PatientSurgicalHistory> findByPatientIdOrderByIdDesc(Long patientId);
}
