package com.medical.medcore.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SystemCatalogRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,49}$",
                message = "El código solo admite letras, números y guion bajo") String code,
        @Size(max = 100) String name,
        Boolean isActive
) {
}
