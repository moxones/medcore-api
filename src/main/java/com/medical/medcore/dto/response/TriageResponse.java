package com.medical.medcore.dto.response;

import java.math.BigDecimal;

public record TriageResponse(
        Long id,
        Long appointmentId,
        BigDecimal weight,
        BigDecimal height,
        BigDecimal temperature,
        Integer heartRate,
        String bloodPressure,
        String notes
) {}
