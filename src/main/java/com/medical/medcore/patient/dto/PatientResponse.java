package com.medical.medcore.patient.dto;

import com.medical.medcore.patient.domain.PortalAccountStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
        Long id,
        String medicalRecordNumber,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        String gender,
        LocalDate birthDate,
        String contactEmail,
        String address,
        String district,
        String documentTypeCode,
        String documentNumber,
        String emergencyContactName,
        String emergencyContactPhone,
        Boolean profileCompleted,
        Boolean hasAccount,
        String userEmail,
        Boolean accountActive,
        PortalAccountStatus accountStatus,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
