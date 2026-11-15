package com.medical.medcore.patient.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortalInvitationResponse(
        Long patientId,
        String email,
        LocalDateTime expiresAt,
        boolean resent,
        boolean delivered,
        String activationToken
) {
}
