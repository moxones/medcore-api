package com.medical.medcore.identity.dto;

import com.medical.medcore.identity.domain.ProfileField;

import java.time.LocalDate;
import java.util.List;

public record AccountProfileResponse(
        String email,
        String firstName,
        String lastName,
        String documentTypeCode,
        String documentNumber,
        LocalDate birthDate,
        String gender,
        String phone,
        String contactEmail,
        String address,
        String district,
        List<ProfileField> requiredFields,
        List<ProfileField> missingFields,
        boolean profileCompleted
) {
}
