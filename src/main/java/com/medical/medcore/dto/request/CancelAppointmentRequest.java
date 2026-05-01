package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelAppointmentRequest(
        @NotBlank String reason
) {}
