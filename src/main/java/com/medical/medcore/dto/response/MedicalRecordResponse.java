package com.medical.medcore.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Historia clínica completa del paciente: base clínica + línea de tiempo de atenciones.
 */
public record MedicalRecordResponse(
        Long recordId,
        Long patientId,
        String patientName,
        String bloodType,
        String allergies,
        String chronicConditions,
        String clinicalNotes,
        LocalDateTime createdAt,
        List<MedicalEntryResponse> entries
) {}
