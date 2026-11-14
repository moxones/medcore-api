package com.medical.medcore.billing.dto;

import com.medical.medcore.billing.domain.PaymentMethod;
import com.medical.medcore.billing.domain.PaymentStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a 0")
        @DecimalMax(value = "99999999.99", message = "El monto excede el máximo permitido")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 2 decimales") BigDecimal amount,
        PaymentMethod paymentMethod,
        @Size(max = 200) String concept,
        @Size(max = 100) String reference,
        PaymentStatus status
) {
}
