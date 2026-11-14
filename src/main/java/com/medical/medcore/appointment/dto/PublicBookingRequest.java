package com.medical.medcore.appointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PublicBookingRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String firstName,
        @NotBlank(message = "El apellido es obligatorio") @Size(max = 100) String lastName,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email inválido") @Size(max = 150) String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
        @NotBlank(message = "El tipo de documento es obligatorio") String documentTypeCode,
        @NotBlank(message = "El número de documento es obligatorio") @Size(max = 50) String documentNumber,
        LocalDate birthDate,
        @Size(max = 50) String phone,
        @NotNull(message = "El médico es obligatorio") Long doctorId,
        @NotNull(message = "La sucursal es obligatoria") Long branchId,
        @NotNull(message = "La fecha y hora son obligatorias") LocalDateTime scheduledAt,
        Long appointmentTypeId,
        @Size(max = 1000) String reason
) {
}
