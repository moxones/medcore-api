package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.FontFamily;
import com.medical.medcore.platform.domain.ThemePreset;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ThemeRequest(
        @NotNull(message = "El tema es obligatorio") ThemePreset preset,
        @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "El color principal debe tener formato #RRGGBB") String primaryColor,
        @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "El color de acento debe tener formato #RRGGBB") String accentColor,
        @NotNull(message = "La tipografía es obligatoria") FontFamily fontFamily
) {
}
