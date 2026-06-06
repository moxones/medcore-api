package com.medical.medcore.dto.response;

import java.time.LocalDateTime;

/**
 * Una cita del día en la agenda del médico (dashboard del portal del médico).
 */
public record DoctorAgendaItemResponse(
        Long appointmentId,
        Long patientId,
        String patientName,
        String patientInitials,
        LocalDateTime scheduledAt,
        Integer durationMinutes,
        String reason,
        String appointmentType,
        String flowStatus,
        boolean isNewPatient
) {}
