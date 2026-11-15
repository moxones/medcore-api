package com.medical.medcore.shared.notification;

import java.util.List;

/**
 * Un correo listo para enviar: versión de texto (siempre) y HTML (opcional), con las imágenes que el HTML referencia
 * por {@code cid:}. {@code senderName} y {@code replyTo} personalizan el remitente con los datos de la organización.
 */
public record Notification(
        Long tenantId,
        String recipient,
        String subject,
        String textBody,
        String htmlBody,
        List<InlineImage> inlineImages,
        String senderName,
        String replyTo
) {

    public Notification {
        inlineImages = inlineImages == null ? List.of() : List.copyOf(inlineImages);
    }

    public static Notification plain(Long tenantId, String recipient, String subject, String textBody) {
        return new Notification(tenantId, recipient, subject, textBody, null, List.of(), null, null);
    }

    public record InlineImage(String contentId, byte[] content, String contentType) {
    }
}
