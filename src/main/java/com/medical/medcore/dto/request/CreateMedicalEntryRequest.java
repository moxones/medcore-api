package com.medical.medcore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateMedicalEntryRequest(
        @NotNull(message = "appointmentId es obligatorio") Long appointmentId,
        String diagnosis,
        String treatment,
        String notes,
        @Valid List<PrescriptionRequest> prescriptions
) {}
