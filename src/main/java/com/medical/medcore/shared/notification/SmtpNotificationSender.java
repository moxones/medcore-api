package com.medical.medcore.shared.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class SmtpNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final InternetAddress defaultFrom;

    public SmtpNotificationSender(JavaMailSender mailSender, String fromAddress) {
        this.mailSender = mailSender;
        this.defaultFrom = parse(fromAddress);
    }

    @Override
    public boolean send(Notification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            boolean multipart = notification.htmlBody() != null;
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, StandardCharsets.UTF_8.name());
            helper.setFrom(sender(notification.senderName()));
            helper.setTo(notification.recipient());
            if (notification.replyTo() != null) {
                helper.setReplyTo(notification.replyTo());
            }
            helper.setSubject(notification.subject());
            if (multipart) {
                helper.setText(notification.textBody(), notification.htmlBody());
                for (Notification.InlineImage image : notification.inlineImages()) {
                    helper.addInline(image.contentId(), new ByteArrayResource(image.content()), image.contentType());
                }
            } else {
                helper.setText(notification.textBody());
            }
            mailSender.send(message);
            return true;
        } catch (MailException | MessagingException | UnsupportedEncodingException failure) {
            log.error("No se pudo enviar el correo a {}: {}", notification.recipient(), failure.getMessage());
            return false;
        }
    }

    @Override
    public boolean canDeliver() {
        return true;
    }

    private InternetAddress sender(String senderName) throws UnsupportedEncodingException {
        if (senderName == null || senderName.isBlank()) {
            return defaultFrom;
        }
        return new InternetAddress(defaultFrom.getAddress(), senderName, StandardCharsets.UTF_8.name());
    }

    private static InternetAddress parse(String fromAddress) {
        try {
            return new InternetAddress(fromAddress, true);
        } catch (AddressException invalid) {
            throw new IllegalStateException("app.mail.from no es una dirección válida: " + fromAddress, invalid);
        }
    }
}
