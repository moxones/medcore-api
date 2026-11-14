package com.medical.medcore.shared.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Slf4j
@RequiredArgsConstructor
public class SmtpNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    @Override
    public void send(Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(notification.recipient());
        message.setSubject(notification.subject());
        message.setText(notification.body());
        try {
            mailSender.send(message);
        } catch (MailException failure) {
            log.error("No se pudo enviar el correo a {}: {}", notification.recipient(), failure.getMessage());
        }
    }
}
