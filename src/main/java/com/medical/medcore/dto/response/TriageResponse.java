package com.medical.medcore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TriageResponse(
        Long id,
        Long appointmentId,
        BigDecimal weight,
        BigDecimal height,
        BigDecimal temperature,
        Integer heartRate,
        Integer respiratoryRate,
        BigDecimal oxygenSaturation,
        String bloodPressure,
        Integer systolicPressure,
        Integer diastolicPressure,
        Integer painScale,
        BigDecimal bloodGlucose,
        BigDecimal bmi,
        String priorityLevel,
        String prioritySystem,
        String notes,
        LocalDateTime measuredAt,
        LocalDateTime createdAt,
        Long createdBy
) {}
