package com.medical.medcore.branch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record BranchRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
        @Size(max = 255) String address,
        @Pattern(regexp = "^$|^\\d{11}$", message = "El RUC debe tener 11 dígitos") String ruc,
        @Size(max = 50) String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        @Min(value = 5, message = "La duración mínima es de 5 minutos")
        @Max(value = 240, message = "La duración máxima es de 240 minutos") Integer appointmentDurationMinutes
) {
}
