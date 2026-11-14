package com.medical.medcore.appointment.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppointmentFlowRequest(@NotBlank(message = "flowStatus es obligatorio") String flowStatus) {
}
