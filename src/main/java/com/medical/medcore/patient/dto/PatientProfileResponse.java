package com.medical.medcore.patient.dto;

import java.time.LocalDate;

public record PatientProfileResponse(
        Long id,
        Long patientId,
        String medicalRecordNumber,
        String firstName,
        String lastName,
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
        boolean profileCompleted,
        boolean hasAccount,
        boolean accountActive
) {
}
