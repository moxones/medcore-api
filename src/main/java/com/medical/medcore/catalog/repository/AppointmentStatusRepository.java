package com.medical.medcore.catalog.repository;

import com.medical.medcore.catalog.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentStatusRepository extends JpaRepository<AppointmentStatus, Long> {

    boolean existsByCode(String code);
}
