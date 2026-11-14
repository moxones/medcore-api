package com.medical.medcore.shared.notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {
        log.info("Notificación sin canal de envío configurado [tenant={}, destinatario={}, asunto={}]",
                notification.tenantId(), notification.recipient(), notification.subject());
    }
}
