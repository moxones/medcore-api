package com.medical.medcore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PublicBookingRequest(
        // Datos de la Persona / Registro
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String documentTypeCode,
        @NotBlank String documentNumber,
        LocalDate birthDate,
        String phone,
        
        // Datos de la Cita Médica
        @NotNull Long doctorId,
        @NotNull Long branchId,
        @NotNull @Future LocalDateTime scheduledAt,
        Long appointmentTypeId,
        String reason
) {}
