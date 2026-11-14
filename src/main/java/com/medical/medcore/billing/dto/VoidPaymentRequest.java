package com.medical.medcore.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidPaymentRequest(
        @NotBlank(message = "El motivo de anulación es obligatorio") @Size(max = 255) String reason
) {
}
