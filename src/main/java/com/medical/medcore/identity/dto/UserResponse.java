package com.medical.medcore.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
        boolean locked,
        boolean pendingActivation,
        @JsonInclude(JsonInclude.Include.NON_NULL) InvitationResponse invitation
) {
}
