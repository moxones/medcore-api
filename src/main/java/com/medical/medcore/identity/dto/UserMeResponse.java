package com.medical.medcore.identity.dto;

import java.util.List;

public record UserMeResponse(
        Long userId,
        Long personId,
        String email,
        List<String> roles,
        String firstName,
        String lastName,
        Long tenantId,
        boolean profileCompleted,
        boolean mustChangePassword,
        List<Long> branchIds
) {
}
