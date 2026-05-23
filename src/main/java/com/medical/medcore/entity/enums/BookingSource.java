package com.medical.medcore.entity.enums;

public enum BookingSource {
    SELF,       // Paciente se agendó desde la app/web
    PHONE,      // Staff agendó por llamada telefónica
    IN_PERSON   // Staff agendó de forma presencial en clínica
}
