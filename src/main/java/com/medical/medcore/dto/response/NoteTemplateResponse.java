package com.medical.medcore.dto.response;

import java.time.LocalDateTime;

public record NoteTemplateResponse(
        Long id,
        String name,
        String specialtyName,
        String chiefComplaint,
        String presentIllness,
        String physicalExamination,
        String assessment,
        String plan,
        String treatment,
        String notes,
        long usageCount,
        LocalDateTime updatedAt
) {}
