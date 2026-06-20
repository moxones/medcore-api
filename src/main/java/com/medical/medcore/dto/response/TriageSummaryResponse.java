package com.medical.medcore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resumen de un triaje del día para la vista "Hoy" del médico
 * (`GET /triage/today`). Combina los signos vitales del triaje con los datos
 * de la cita, el paciente y el asistente que lo registró.
 */
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
