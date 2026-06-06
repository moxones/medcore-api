package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OrderRequest(
        @NotBlank(message = "El tipo de orden es obligatorio") String orderType,
        @NotBlank(message = "La descripción de la orden es obligatoria") String description
) {}
