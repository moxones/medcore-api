package com.medical.medcore.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PortalInvitationRequest(@Email(message = "Email inválido") @Size(max = 150) String email) {
}
