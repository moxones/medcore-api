package com.medical.medcore.doctor.dto;

import jakarta.validation.constraints.Size;

public record DoctorRequest(
        PersonRef person,
        Long personId,
        @Size(max = 50, message = "La matrícula no puede superar los 50 caracteres") String licenseNumber,
        Boolean isActive
) {

    public record PersonRef(Long id) {
    }

    public Long resolvedPersonId() {
        if (personId != null) {
            return personId;
        }
        return person != null ? person.id() : null;
    }
}
