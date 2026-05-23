package com.medical.medcore.repository;

import com.medical.medcore.entity.AppointmentReschedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRescheduleRepository extends JpaRepository<AppointmentReschedule, Long> {
}
