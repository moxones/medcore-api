package com.medical.medcore.billing.dto;

import java.math.BigDecimal;
import java.util.List;

public record AppointmentBalanceResponse(
        Long appointmentId,
        BigDecimal price,
        BigDecimal paid,
        BigDecimal pending,
        List<PaymentResponse> payments
) {
}
