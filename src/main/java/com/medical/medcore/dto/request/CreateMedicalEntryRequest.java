package com.medical.medcore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateMedicalEntryRequest(
        @NotNull(message = "appointmentId es obligatorio") Long appointmentId,
        String entryType,
        String chiefComplaint,
        String presentIllness,
        String reviewOfSystems,
        String physicalExamination,
        String assessment,
        String plan,
        String diagnosis,
        String treatment,
        String notes,
        LocalDate followUpAt,
        @Valid List<PrescriptionRequest> prescriptions,
        @Valid List<DiagnosisRequest> diagnoses,
        @Valid List<ProcedureRequest> procedures,
        @Valid List<OrderRequest> orders,
        @Valid List<CertificateRequest> certificates
) {}
