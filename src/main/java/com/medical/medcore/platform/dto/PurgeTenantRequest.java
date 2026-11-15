package com.medical.medcore.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record PurgeTenantRequest(@NotBlank(message = "La confirmación es obligatoria") String confirmation) {
}
