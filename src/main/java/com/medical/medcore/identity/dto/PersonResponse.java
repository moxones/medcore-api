package com.medical.medcore.identity.dto;

import java.time.LocalDate;

public record PersonResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String gender,
        String phone,
        String contactEmail,
        String address,
        String district,
        boolean profileCompleted,
        String documentTypeCode,
        String documentNumber
) {
}
