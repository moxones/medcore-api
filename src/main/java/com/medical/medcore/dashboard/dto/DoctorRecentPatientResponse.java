package com.medical.medcore.dashboard.dto;

import java.time.LocalDateTime;

public record DoctorRecentPatientResponse(
        Long patientId,
        String patientName,
        String patientInitials,
        LocalDateTime lastVisitAt,
        String lastReason
) {}
