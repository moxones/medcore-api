package com.medical.medcore.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateSubscriptionRequest(
        @NotNull(message = "El plan es obligatorio") Long planId,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate startDate,
        LocalDate endDate,
        @NotBlank(message = "El estado es obligatorio") String status
) {
}
