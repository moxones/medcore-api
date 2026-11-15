package com.medical.medcore.identity.dto;

import com.medical.medcore.identity.domain.ProfileField;

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
        List<ProfileField> missingProfileFields,
        boolean mustChangePassword,
        List<Long> branchIds
) {
}
