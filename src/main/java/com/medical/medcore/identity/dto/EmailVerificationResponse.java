package com.medical.medcore.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code delivered = false}: el correo no salió y el registro puede seguir sin código. {@code code} solo viaja en
 * desarrollo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailVerificationResponse(boolean delivered, Integer expiresInMinutes, Integer resendAfterSeconds,
                                        String code) {
}
