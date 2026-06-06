package com.medical.medcore.repository;

import com.medical.medcore.entity.Triage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriageRepository extends JpaRepository<Triage, Long> {

    List<Triage> findByAppointmentIdOrderByMeasuredAtDescIdDesc(Long appointmentId);

    Optional<Triage> findFirstByAppointmentIdOrderByMeasuredAtDescIdDesc(Long appointmentId);
}
