package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TriageRequest(
        @NotNull(message = "appointmentId es obligatorio") Long appointmentId,
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
        LocalDateTime measuredAt
) {}
