package com.medical.medcore.shared.notification;

import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Plantillas de los correos de cuenta. Un solo diseño base (encabezado con la marca de la organización, título,
 * texto, una acción y pie) en HTML con estilos en línea y tablas, que es lo único que respetan Outlook y Gmail, más
 * su versión de texto plano.
 */
public final class MailTemplates {

    public enum CodePurpose {
        PASSWORD_RESET,
        EMAIL_VERIFICATION
    }

    static final String LOGO_CID = "org-logo";

    private static final String DEFAULT_COLOR = "#005CBB";
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final double LIGHT_LUMINANCE = 0.6;
    private static final String PLATFORM = "MedCore";

    private MailTemplates() {
    }

    public static Notification activation(MailBranding brand, Long tenantId, String recipient, String firstName,
                                          String link, int validHours, boolean patient) {
        String org = brand.organizationName();
        String greeting = firstName == null || firstName.isBlank() ? "¡Hola!" : "¡Hola, " + capitalize(firstName) + "!";
        String intro = patient
                ? org + " te invitó a su portal de pacientes. Crea tu contraseña para ver tus citas y tus datos en línea."
                : org + " creó tu cuenta de acceso. Crea tu contraseña para empezar a usar el sistema.";
        String expiry = "El enlace vence en " + validHours + " horas y solo se puede usar una vez.";
        String subject = patient ? "Activa tu cuenta en " + org : "Tu acceso a " + org;

        String html = layout(brand, greeting,
                paragraph(intro)
                        + button(brand, "Activar mi cuenta", link)
                        + small(expiry)
                        + small("Si el botón no funciona, copia y pega este enlace en tu navegador:")
                        + linkText(link)
                        + small("Si no esperabas este correo, puedes ignorarlo."));
        String text = greeting + "\n\n" + intro + "\n\nActiva tu cuenta aquí:\n" + link + "\n\n" + expiry
                + "\nSi no esperabas este correo, puedes ignorarlo." + textFooter(org);
        return build(brand, tenantId, recipient, subject, text, html);
    }

    public static Notification code(MailBranding brand, Long tenantId, String recipient, CodePurpose purpose,
                                    String code, int validMinutes) {
        String org = brand.organizationName();
        boolean reset = purpose == CodePurpose.PASSWORD_RESET;
        String title = reset ? "Restablece tu contraseña" : "Verifica tu correo";
        String intro = reset
                ? "Recibimos una solicitud para restablecer tu contraseña en " + org + ". Usa este código:"
                : "Estás creando tu cuenta en " + org + ". Usa este código para confirmar tu correo:";
        String expiry = "El código vence en " + validMinutes + " minutos.";
        String ignore = reset
                ? "Si no lo pediste, ignora este correo: tu contraseña no cambia."
                : "Si no estás creando una cuenta, ignora este correo.";
        String subject = code + (reset ? " es tu código para restablecer la contraseña" : " es tu código de verificación");

        String html = layout(brand, title,
                paragraph(intro) + codeBox(brand, code) + small(expiry) + small(ignore));
        String text = title + "\n\n" + intro + "\n\n    " + code + "\n\n" + expiry + "\n" + ignore + textFooter(org);
        return build(brand, tenantId, recipient, subject, text, html);
    }

    private static Notification build(MailBranding brand, Long tenantId, String recipient, String subject,
                                      String text, String html) {
        List<Notification.InlineImage> images = brand.hasLogo()
                ? List.of(new Notification.InlineImage(LOGO_CID, brand.logo(), brand.logoContentType()))
                : List.of();
        String senderName = brand.organizationName() + " vía " + PLATFORM;
        return new Notification(tenantId, recipient, subject, text, html, images, senderName, brand.replyTo());
    }

    private static String layout(MailBranding brand, String title, String content) {
        String org = escape(brand.organizationName());
        return """
                <!doctype html>
                <html lang="es">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>%s</title></head>
                <body style="margin:0;padding:0;background:#f3f4f6;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:32px 12px;">
                <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                <tr><td style="height:6px;background:%s;font-size:0;line-height:0;">&nbsp;</td></tr>
                <tr><td align="center" style="padding:28px 32px 8px;">%s</td></tr>
                <tr><td style="padding:8px 32px 32px;">
                <h1 style="margin:0 0 16px;font-size:22px;line-height:1.3;color:#111827;text-align:center;">%s</h1>
                %s
                </td></tr>
                <tr><td style="padding:18px 32px;background:#f9fafb;border-top:1px solid #e5e7eb;font-size:12px;line-height:1.6;color:#6b7280;text-align:center;">
                Enviado por <strong style="color:#374151;">%s</strong> a través de %s.<br>Este es un mensaje automático, por favor no respondas a este correo.
                </td></tr>
                </table>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(escape(title), color(brand), header(brand), escape(title), content, org, PLATFORM);
    }

    private static String header(MailBranding brand) {
        String org = escape(brand.organizationName());
        if (brand.hasLogo()) {
            return "<img src=\"cid:" + LOGO_CID + "\" alt=\"" + org + "\" height=\"56\" "
                    + "style=\"display:block;max-height:56px;max-width:220px;height:56px;width:auto;border:0;\">";
        }
        return "<span style=\"font-size:22px;font-weight:700;letter-spacing:-0.2px;color:" + color(brand) + ";\">"
                + org + "</span>";
    }

    private static String paragraph(String text) {
        return "<p style=\"margin:0 0 20px;font-size:15px;line-height:1.6;color:#374151;text-align:center;\">"
                + escape(text) + "</p>";
    }

    private static String small(String text) {
        return "<p style=\"margin:0 0 8px;font-size:13px;line-height:1.5;color:#6b7280;text-align:center;\">"
                + escape(text) + "</p>";
    }

    private static String linkText(String link) {
        return "<p style=\"margin:0 0 16px;font-size:12px;line-height:1.5;text-align:center;word-break:break-all;\">"
                + "<a href=\"" + escape(link) + "\" style=\"color:#4b5563;\">" + escape(link) + "</a></p>";
    }

    private static String button(MailBranding brand, String label, String link) {
        String background = color(brand);
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" align="center" style="margin:8px auto 24px;">
                <tr><td align="center" bgcolor="%s" style="border-radius:10px;background:%s;">
                <a href="%s" target="_blank" style="display:inline-block;padding:14px 32px;font-size:16px;font-weight:600;color:%s;text-decoration:none;border-radius:10px;">%s</a>
                </td></tr>
                </table>
                """.formatted(background, background, escape(link), foreground(background), escape(label));
    }

    private static String codeBox(MailBranding brand, String code) {
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" align="center" style="margin:4px auto 20px;">
                <tr><td align="center" style="padding:16px 28px;border:2px dashed %s;border-radius:12px;background:#f9fafb;">
                <span style="font-family:Consolas,'Courier New',monospace;font-size:34px;font-weight:700;letter-spacing:10px;color:#111827;">%s</span>
                </td></tr>
                </table>
                """.formatted(color(brand), escape(code));
    }

    private static String textFooter(String org) {
        return "\n\n--\nEnviado por " + org + " a través de " + PLATFORM + ". Este es un mensaje automático.";
    }

    static String color(MailBranding brand) {
        String candidate = brand.primaryColor();
        return candidate != null && HEX_COLOR.matcher(candidate).matches() ? candidate : DEFAULT_COLOR;
    }

    static String foreground(String hexBackground) {
        int red = Integer.parseInt(hexBackground.substring(1, 3), 16);
        int green = Integer.parseInt(hexBackground.substring(3, 5), 16);
        int blue = Integer.parseInt(hexBackground.substring(5, 7), 16);
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        return luminance > LIGHT_LUMINANCE ? "#111827" : "#ffffff";
    }

    private static String capitalize(String name) {
        String trimmed = name.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value, "UTF-8");
    }
}
