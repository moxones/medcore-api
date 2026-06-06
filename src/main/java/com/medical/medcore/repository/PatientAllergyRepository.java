package com.medical.medcore.repository;

import com.medical.medcore.entity.PatientAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, Long> {
    List<PatientAllergy> findByPatientIdOrderByIdDesc(Long patientId);

    @Query("SELECT a.patientId, COUNT(a) FROM PatientAllergy a " +
           "WHERE a.patientId IN :patientIds AND a.isActive = true GROUP BY a.patientId")
    List<Object[]> countActiveByPatientIds(@Param("patientIds") List<Long> patientIds);
}
