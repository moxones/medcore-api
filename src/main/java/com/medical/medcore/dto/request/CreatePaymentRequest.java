package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "El monto es obligatorio") @Positive(message = "El monto debe ser mayor a 0") BigDecimal amount,
        String paymentMethod,
        String concept,
        String status
) {}
