package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.FontFamily;
import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.ThemePreset;

import java.util.Objects;

public record TenantThemeResponse(
        ThemePreset preset,
        String primaryColor,
        String accentColor,
        FontFamily fontFamily,
        String fontName,
        String customPrimaryColor,
        String customAccentColor
) {

    public static TenantThemeResponse from(Tenant tenant) {
        ThemePreset preset = effectivePreset(tenant);
        String primary = preset.custom() ? tenant.getPrimaryColor() : preset.primaryColor();
        String accent = preset.custom() ? Objects.requireNonNullElse(tenant.getAccentColor(), primary)
                : preset.accentColor();
        return new TenantThemeResponse(preset, primary, accent, tenant.getFontFamily(),
                tenant.getFontFamily().fontName(), tenant.getPrimaryColor(), tenant.getAccentColor());
    }

    private static ThemePreset effectivePreset(Tenant tenant) {
        if (tenant.getThemePreset().custom() && tenant.getPrimaryColor() == null) {
            return ThemePreset.CLASSIC;
        }
        return tenant.getThemePreset();
    }
}
