package com.medical.medcore.shared.notification;

public interface NotificationSender {

    /**
     * Envía el correo y devuelve {@code true} solo si el servidor de correo lo aceptó. Nunca lanza excepción: quien
     * llama decide qué hacer si no salió (mostrar el enlace, omitir la verificación, etc.).
     */
    boolean send(Notification notification);

    /** {@code false} cuando no hay canal real configurado: nada de lo que se envíe llegará a la persona. */
    boolean canDeliver();
}
