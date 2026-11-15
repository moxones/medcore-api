package com.medical.medcore.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Resultado de enviar una invitación de activación. Si {@code delivered} es {@code false} el correo no salió y
 * {@code activationToken} viene para compartir el enlace por otro medio (en desarrollo viene siempre).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvitationResponse(String email, LocalDateTime expiresAt, boolean delivered, String activationToken) {
}
