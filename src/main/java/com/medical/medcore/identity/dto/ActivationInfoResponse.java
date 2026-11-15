package com.medical.medcore.identity.dto;

import java.time.LocalDateTime;

public record ActivationInfoResponse(String firstName, String email, LocalDateTime expiresAt) {
}
