package com.medical.medcore.platform.domain;

public enum FontFamily {
    ROBOTO("Roboto", "Neutra y legible, la de siempre."),
    INTER("Inter", "Moderna y nítida en pantallas."),
    POPPINS("Poppins", "Geométrica y amigable."),
    NUNITO("Nunito", "Redondeada y cálida."),
    LATO("Lato", "Clásica y profesional."),
    MONTSERRAT("Montserrat", "Elegante y con presencia.");

    private final String fontName;
    private final String description;

    FontFamily(String fontName, String description) {
        this.fontName = fontName;
        this.description = description;
    }

    public String fontName() {
        return fontName;
    }

    public String description() {
        return description;
    }
}
