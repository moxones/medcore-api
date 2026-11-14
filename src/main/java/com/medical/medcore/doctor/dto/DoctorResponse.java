package com.medical.medcore.doctor.dto;

import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.identity.domain.Person;

import java.time.LocalDateTime;

public record DoctorResponse(
        Long id,
        String licenseNumber,
        Boolean isActive,
        PersonSummary person,
        LocalDateTime createdAt
) {

    public record PersonSummary(Long id, String firstName, String lastName, String phone, String contactEmail) {
    }

    public static DoctorResponse from(Doctor doctor) {
        Person person = doctor.getPerson();
        return new DoctorResponse(
                doctor.getId(),
                doctor.getLicenseNumber(),
                doctor.getIsActive(),
                new PersonSummary(person.getId(), person.getFirstName(), person.getLastName(),
                        person.getPhone(), person.getContactEmail()),
                doctor.getCreatedAt());
    }
}
