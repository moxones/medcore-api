package com.medical.medcore.doctor.dto;

import com.medical.medcore.clinical.dto.PrescriptionResponse;

import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionDocumentResponse(
        Long entryId,
        Long appointmentId,
        Long patientId,
        String patientName,
        String patientInitials,
        LocalDateTime issuedAt,
        String diagnosisSummary,
        Boolean isLocked,
        List<PrescriptionResponse> items
) {}
