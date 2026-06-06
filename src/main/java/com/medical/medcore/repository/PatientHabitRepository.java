package com.medical.medcore.repository;

import com.medical.medcore.entity.PatientHabit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientHabitRepository extends JpaRepository<PatientHabit, Long> {
    List<PatientHabit> findByPatientIdOrderByIdDesc(Long patientId);
}
