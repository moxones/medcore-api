package com.medical.medcore.repository;

import com.medical.medcore.entity.MedicalOrderResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalOrderResultRepository extends JpaRepository<MedicalOrderResult, Long> {
    List<MedicalOrderResult> findByMedicalOrderIdOrderByIdAsc(Long medicalOrderId);
    List<MedicalOrderResult> findByMedicalOrderIdInOrderByIdAsc(List<Long> medicalOrderIds);
}
