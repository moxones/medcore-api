package com.medical.medcore.appointment.service;

import com.medical.medcore.appointment.domain.Appointment;

public interface AppointmentSettlement {

    boolean hasPendingBalance(Appointment appointment);
}
