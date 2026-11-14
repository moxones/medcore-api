package com.medical.medcore.appointment.repository;

import com.medical.medcore.appointment.domain.AppointmentFlowHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentFlowHistoryRepository extends JpaRepository<AppointmentFlowHistory, Long> {

    List<AppointmentFlowHistory> findByAppointmentIdOrderByIdAsc(Long appointmentId);
}
