package com.medical.medcore.dto.request;

import java.math.BigDecimal;

public record TriageRequest(
        Long appointmentId,
        BigDecimal weight,
        BigDecimal height,
        BigDecimal temperature,
        Integer heartRate,
        String bloodPressure,
        String notes
) {}
