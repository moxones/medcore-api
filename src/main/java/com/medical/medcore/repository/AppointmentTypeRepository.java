package com.medical.medcore.repository;

import com.medical.medcore.entity.AppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentTypeRepository extends JpaRepository<AppointmentType, Long> {
    List<AppointmentType> findByIsActiveTrue();
    Optional<AppointmentType> findByCode(String code);
    boolean existsByCode(String code);
}
