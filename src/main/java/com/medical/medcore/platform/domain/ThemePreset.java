package com.medical.medcore.platform.domain;

public enum ThemePreset {
    CLASSIC("Clásico", "El estilo original de MedCore: azul con acento turquesa.", "#005CBB", "#0D9488"),
    OCEAN("Océano", "Turquesa sereno, transmite calma y limpieza.", "#0E7490", "#0D9488"),
    ROYAL("Real", "Azul profundo con un toque violeta, sobrio y confiable.", "#1D4ED8", "#7C3AED"),
    FOREST("Bosque", "Verde natural, cercano y saludable.", "#15803D", "#0D9488"),
    LAVENDER("Lavanda", "Violeta suave, cálido y acogedor.", "#6D28D9", "#DB2777"),
    CORAL("Coral", "Rojo coral con energía, cercano y vital.", "#BE123C", "#F97316"),
    AMBER("Ámbar", "Tonos tierra, cálidos y acogedores.", "#B45309", "#DC2626"),
    GRAPHITE("Grafito", "Gris elegante con acento celeste, minimalista.", "#334155", "#0EA5E9"),
    CUSTOM("Personalizado", "Tus propios colores de marca.", null, null);

    private final String label;
    private final String description;
    private final String primaryColor;
    private final String accentColor;

    ThemePreset(String label, String description, String primaryColor, String accentColor) {
        this.label = label;
        this.description = description;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public String primaryColor() {
        return primaryColor;
    }

    public String accentColor() {
        return accentColor;
    }

    public boolean custom() {
        return this == CUSTOM;
    }
}
