package com.medical.medcore.repository;

import com.medical.medcore.entity.PatientCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientConditionRepository extends JpaRepository<PatientCondition, Long> {
    List<PatientCondition> findByPatientIdOrderByIdDesc(Long patientId);

    @Query("SELECT c.patientId, COUNT(c) FROM PatientCondition c " +
           "WHERE c.patientId IN :patientIds GROUP BY c.patientId")
    List<Object[]> countByPatientIds(@Param("patientIds") List<Long> patientIds);
}
