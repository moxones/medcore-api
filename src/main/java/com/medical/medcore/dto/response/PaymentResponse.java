package com.medical.medcore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long appointmentId,
        BigDecimal amount,
        String status,
        String paymentMethod,
        String concept,
        LocalDateTime paymentDate,
        LocalDateTime createdAt,
        Long createdBy
) {}
