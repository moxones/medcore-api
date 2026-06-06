package com.medical.medcore.dto.response;

import java.time.LocalDateTime;

/**
 * Paciente atendido recientemente por el médico (dashboard del portal del médico).
 */
public record DoctorRecentPatientResponse(
        Long patientId,
        String patientName,
        String patientInitials,
        LocalDateTime lastVisitAt,
        String lastReason
) {}
