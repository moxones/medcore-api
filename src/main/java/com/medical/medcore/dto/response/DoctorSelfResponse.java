package com.medical.medcore.dto.response;

import com.medical.medcore.entity.Doctor;

import java.time.LocalDateTime;

/**
 * Registro del médico autenticado (`GET /doctors/me`). Expone únicamente los
 * campos públicos del doctor; evita serializar la entidad JPA y su relación
 * perezosa `person`.
 */
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
