package com.medical.medcore.appointment.repository;

import com.medical.medcore.appointment.domain.AppointmentReschedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRescheduleRepository extends JpaRepository<AppointmentReschedule, Long> {
}
