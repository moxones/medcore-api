package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PatientSurgicalHistoryRequest(
        @NotBlank(message = "El procedimiento es obligatorio") String procedure,
        LocalDate performedOn,
        String notes
) {}
