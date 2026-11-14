package com.medical.medcore.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForgotPasswordResponse(String resetToken, Integer expiresInMinutes) {
}
