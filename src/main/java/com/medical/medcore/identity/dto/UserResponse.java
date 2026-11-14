package com.medical.medcore.identity.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        Boolean isActive,
        PersonResponse person,
        List<String> roles,
        List<Long> roleIds,
        Long tenantId,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        boolean locked
) {
}
