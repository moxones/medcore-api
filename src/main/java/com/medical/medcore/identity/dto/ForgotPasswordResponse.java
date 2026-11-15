package com.medical.medcore.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Respuesta idéntica exista o no la cuenta, para no revelar quién está registrado. {@code resetCode} solo viaja en
 * desarrollo ({@code app.auth.expose-reset-token}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForgotPasswordResponse(String resetCode, Integer expiresInMinutes, Integer resendAfterSeconds) {
}
