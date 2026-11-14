package com.medical.medcore.doctor.dto;

import com.medical.medcore.doctor.domain.Doctor;

import java.time.LocalDateTime;

public record DoctorSelfResponse(
        Long id,
        Long tenantId,
        String licenseNumber,
        Boolean isActive,
        LocalDateTime createdAt
) {
    public static DoctorSelfResponse from(Doctor doctor) {
        return new DoctorSelfResponse(
                doctor.getId(),
                doctor.getTenantId(),
                doctor.getLicenseNumber(),
                doctor.getIsActive(),
                doctor.getCreatedAt()
        );
    }
}
