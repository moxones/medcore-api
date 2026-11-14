package com.medical.medcore.identity.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull(message = "isActive es obligatorio") Boolean isActive) {
}
