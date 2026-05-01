package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppointmentFlowRequest(
        @NotBlank String flowStatus
) {}
