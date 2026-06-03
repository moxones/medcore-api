package com.medical.medcore.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MedicalEntryResponse(
        Long id,
        Long appointmentId,
        String diagnosis,
        String treatment,
        String notes,
        LocalDateTime createdAt,
        Long createdBy,
        String createdByName,
        List<PrescriptionResponse> prescriptions
) {}
