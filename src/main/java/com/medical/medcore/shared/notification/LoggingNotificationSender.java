package com.medical.medcore.shared.notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public boolean send(Notification notification) {
        log.info("Correo sin canal de envío configurado [tenant={}, destinatario={}, asunto={}]",
                notification.tenantId(), notification.recipient(), notification.subject());
        log.debug("Contenido del correo no enviado:\n{}", notification.textBody());
        return false;
    }

    @Override
    public boolean canDeliver() {
        return false;
    }
}
