package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record ProcedureRequest(
        String code,
        @NotBlank(message = "El nombre del procedimiento es obligatorio") String name,
        String notes,
        LocalDateTime performedAt
) {}
