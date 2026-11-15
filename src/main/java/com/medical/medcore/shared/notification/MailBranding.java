package com.medical.medcore.shared.notification;

/**
 * Lo que un correo necesita saber de la organización que lo envía: nombre, color de marca, logo (PNG o JPG, que es
 * lo que muestran todos los clientes de correo) y la dirección web donde la persona abre los enlaces.
 */
public record MailBranding(
        String organizationName,
        String primaryColor,
        byte[] logo,
        String logoContentType,
        String baseUrl,
        String replyTo
) {

    public boolean hasLogo() {
        return logo != null && logo.length > 0 && logoContentType != null;
    }

    public String link(String path) {
        return baseUrl.replaceAll("/+$", "") + path;
    }
}
