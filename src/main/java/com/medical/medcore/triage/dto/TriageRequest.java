package com.medical.medcore.triage.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TriageRequest(
        @NotNull(message = "appointmentId es obligatorio") Long appointmentId,
        @DecimalMin(value = "0.5", message = "Peso fuera de rango") @DecimalMax(value = "500", message = "Peso fuera de rango") BigDecimal weight,
        @DecimalMin(value = "0.2", message = "Talla fuera de rango") @DecimalMax(value = "300", message = "Talla fuera de rango") BigDecimal height,
        @DecimalMin(value = "25", message = "Temperatura fuera de rango") @DecimalMax(value = "45", message = "Temperatura fuera de rango") BigDecimal temperature,
        @Min(value = 20, message = "Frecuencia cardíaca fuera de rango") @Max(value = 300, message = "Frecuencia cardíaca fuera de rango") Integer heartRate,
        @Min(value = 4, message = "Frecuencia respiratoria fuera de rango") @Max(value = 80, message = "Frecuencia respiratoria fuera de rango") Integer respiratoryRate,
        @DecimalMin(value = "0", message = "Saturación fuera de rango") @DecimalMax(value = "100", message = "Saturación fuera de rango") BigDecimal oxygenSaturation,
        @Size(max = 20) String bloodPressure,
        @Min(value = 40, message = "Presión sistólica fuera de rango") @Max(value = 300, message = "Presión sistólica fuera de rango") Integer systolicPressure,
        @Min(value = 20, message = "Presión diastólica fuera de rango") @Max(value = 200, message = "Presión diastólica fuera de rango") Integer diastolicPressure,
        @Min(value = 0, message = "La escala de dolor va de 0 a 10") @Max(value = 10, message = "La escala de dolor va de 0 a 10") Integer painScale,
        @DecimalMin(value = "10", message = "Glucosa fuera de rango") @DecimalMax(value = "999", message = "Glucosa fuera de rango") BigDecimal bloodGlucose,
        @DecimalMin(value = "5", message = "IMC fuera de rango") @DecimalMax(value = "150", message = "IMC fuera de rango") BigDecimal bmi,
        @Size(max = 20) String priorityLevel,
        @Pattern(regexp = "ESI|MANCHESTER", message = "prioritySystem debe ser ESI o MANCHESTER") String prioritySystem,
        @Size(max = 2000) String notes,
        LocalDateTime measuredAt
) {
}
