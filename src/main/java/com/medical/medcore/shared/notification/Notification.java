package com.medical.medcore.shared.notification;

public record Notification(Long tenantId, String recipient, String subject, String body) {
}
