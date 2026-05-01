package com.medical.medcore.dto.response;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        String patientName,
        String doctorName,
        String branchName,
        LocalDateTime scheduledAt,
        Long statusId,
        String flowStatus
) {}
