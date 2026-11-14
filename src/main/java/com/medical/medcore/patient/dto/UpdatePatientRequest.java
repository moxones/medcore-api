package com.medical.medcore.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdatePatientRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        LocalDate birthDate,
        @Size(max = 20) String gender,
        @Size(max = 50) String phone,
        @Email(message = "Email inválido") @Size(max = 150) String contactEmail,
        @Size(max = 255) String address,
        @Size(max = 100) String district,
        @Size(max = 20) String documentTypeCode,
        @Size(max = 50) String documentNumber,
        @Size(max = 150) String emergencyContactName,
        @Size(max = 50) String emergencyContactPhone,
        Boolean isActive
) {
}
