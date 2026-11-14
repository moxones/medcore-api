package com.medical.medcore.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientSelfRegistrationRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String firstName,
        @NotBlank(message = "El apellido es obligatorio") @Size(max = 100) String lastName,
        LocalDate birthDate,
        @Size(max = 50) String phone,
        @NotBlank(message = "El tipo de documento es obligatorio") String documentTypeCode,
        @NotBlank(message = "El número de documento es obligatorio") @Size(max = 50) String documentNumber,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email inválido") @Size(max = 150) String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password
) {
}
