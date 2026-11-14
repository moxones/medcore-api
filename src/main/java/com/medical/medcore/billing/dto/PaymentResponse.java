package com.medical.medcore.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long appointmentId,
        BigDecimal amount,
        String status,
        String paymentMethod,
        String concept,
        String reference,
        LocalDateTime paymentDate,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime voidedAt,
        String voidReason,
        String appointmentFlowStatus
) {
}
