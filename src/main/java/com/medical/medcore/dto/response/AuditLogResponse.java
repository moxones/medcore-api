package com.medical.medcore.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String tableName,
        Long recordId,
        String action,
        String oldValues,
        String newValues,
        Long changedBy,
        LocalDateTime changedAt,
        String ipAddress
) {}
