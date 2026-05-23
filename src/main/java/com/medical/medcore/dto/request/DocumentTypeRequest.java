package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DocumentTypeRequest(
        @NotBlank(message = "El código es obligatorio") String code,
        @NotBlank(message = "El nombre es obligatorio") String name,
        Boolean isActive
) {}
