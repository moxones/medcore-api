package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.FontFamily;
import com.medical.medcore.platform.domain.ThemePreset;

import java.util.Arrays;
import java.util.List;

public record ThemeOptionsResponse(List<PresetOption> presets, List<FontOption> fonts) {

    public static ThemeOptionsResponse all() {
        return new ThemeOptionsResponse(
                Arrays.stream(ThemePreset.values()).map(PresetOption::from).toList(),
                Arrays.stream(FontFamily.values()).map(FontOption::from).toList());
    }

    public record PresetOption(ThemePreset code, String label, String description, String primaryColor,
                               String accentColor) {

        static PresetOption from(ThemePreset preset) {
            return new PresetOption(preset, preset.label(), preset.description(), preset.primaryColor(),
                    preset.accentColor());
        }
    }

    public record FontOption(FontFamily code, String fontName, String description) {

        static FontOption from(FontFamily font) {
            return new FontOption(font, font.fontName(), font.description());
        }
    }
}
