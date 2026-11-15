package com.medical.medcore.shared.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailTemplatesTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};

    private static MailBranding brand(String color, byte[] logo) {
        return new MailBranding("Clínica <Perrito>", color, logo, logo != null ? "image/png" : null,
                "http://perrito.localhost:4200/", "contacto@perrito.pe");
    }

    @Test
    void activationEmailCarriesTheButtonLinkInHtmlAndText() {
        Notification mail = MailTemplates.activation(brand("#15803D", null), 7L, "ana@correo.pe", "ANA",
                "http://perrito.localhost:4200/activate-account?token=abc", 72, true);

        assertThat(mail.subject()).isEqualTo("Activa tu cuenta en Clínica <Perrito>");
        assertThat(mail.htmlBody()).contains("href=\"http://perrito.localhost:4200/activate-account?token=abc\"")
                .contains("Activar mi cuenta").contains("¡Hola, Ana!").contains("#15803D");
        assertThat(mail.textBody()).contains("http://perrito.localhost:4200/activate-account?token=abc");
        assertThat(mail.senderName()).isEqualTo("Clínica <Perrito> vía MedCore");
        assertThat(mail.replyTo()).isEqualTo("contacto@perrito.pe");
    }

    @Test
    void organizationDataIsEscapedInTheHtml() {
        Notification mail = MailTemplates.code(brand(null, null), 7L, "ana@correo.pe",
                MailTemplates.CodePurpose.PASSWORD_RESET, "012345", 10);

        assertThat(mail.htmlBody()).contains("Clínica &lt;Perrito&gt;").doesNotContain("<Perrito>");
    }

    @Test
    void codeEmailShowsTheCodeAndPutsItInTheSubject() {
        Notification mail = MailTemplates.code(brand("#1D4ED8", null), 7L, "ana@correo.pe",
                MailTemplates.CodePurpose.EMAIL_VERIFICATION, "012345", 10);

        assertThat(mail.subject()).startsWith("012345 ");
        assertThat(mail.htmlBody()).contains("012345").contains("10 minutos");
        assertThat(mail.textBody()).contains("012345");
    }

    @Test
    void logoTravelsAsInlineImageOnlyWhenPresent() {
        Notification withLogo = MailTemplates.code(brand("#1D4ED8", PNG), 7L, "a@b.pe",
                MailTemplates.CodePurpose.PASSWORD_RESET, "111111", 10);
        Notification withoutLogo = MailTemplates.code(brand("#1D4ED8", null), 7L, "a@b.pe",
                MailTemplates.CodePurpose.PASSWORD_RESET, "111111", 10);

        assertThat(withLogo.inlineImages()).hasSize(1);
        assertThat(withLogo.htmlBody()).contains("cid:" + MailTemplates.LOGO_CID);
        assertThat(withoutLogo.inlineImages()).isEmpty();
        assertThat(withoutLogo.htmlBody()).doesNotContain("cid:");
    }

    @Test
    void invalidColorsFallBackToTheDefaultAndLightColorsGetDarkText() {
        assertThat(MailTemplates.color(brand("rojo", null))).isEqualTo("#005CBB");
        assertThat(MailTemplates.foreground("#FDE68A")).isEqualTo("#111827");
        assertThat(MailTemplates.foreground("#005CBB")).isEqualTo("#ffffff");
    }
}
