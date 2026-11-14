package com.medical.medcore.shared.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class NotificationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    public NotificationSender smtpNotificationSender(JavaMailSender mailSender,
                                                     @Value("${app.mail.from:no-reply@medcore.local}") String from) {
        return new SmtpNotificationSender(mailSender, from);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationSender.class)
    public NotificationSender loggingNotificationSender() {
        return new LoggingNotificationSender();
    }
}
