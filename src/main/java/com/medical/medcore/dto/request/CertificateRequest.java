package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CertificateRequest(
        @NotBlank(message = "El tipo de certificado es obligatorio") String certificateType,
        @NotBlank(message = "El contenido del certificado es obligatorio") String content,
        Integer restDays,
        LocalDate validUntil
) {}
