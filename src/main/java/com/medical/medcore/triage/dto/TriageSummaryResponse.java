package com.medical.medcore.triage.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TriageSummaryResponse(
        Long id,
        Long appointmentId,
        Long patientId,
        String patientName,
        String patientPhone,
        LocalDate patientBirthDate,
        Integer patientAge,
        String patientGender,
        String bloodType,
        String allergies,
        String chronicConditions,
        Long doctorId,
        String doctorName,
        String appointmentTypeName,
        LocalDateTime scheduledAt,
        LocalDateTime createdAt,
        String urgencyLevel,
        BigDecimal weight,
        BigDecimal height,
        BigDecimal temperature,
        Integer heartRate,
        String bloodPressure,
        BigDecimal oxygenSaturation,
        Integer respiratoryRate,
        Integer painScale,
        String notes,
        String assistantName
) {}
