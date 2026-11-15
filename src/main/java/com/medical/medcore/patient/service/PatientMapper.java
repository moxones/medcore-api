package com.medical.medcore.patient.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.PersonDocument;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.domain.PortalAccountStatus;
import com.medical.medcore.patient.dto.PatientProfileResponse;
import com.medical.medcore.patient.dto.PatientResponse;

public final class PatientMapper {

    private PatientMapper() {
    }

    public static PatientResponse toResponse(Patient patient, User account) {
        Person person = patient.getPerson();
        PersonDocument document = person.primaryDocument();
        return new PatientResponse(
                patient.getId(),
                patient.getMedicalRecordNumber(),
                person.getFirstName(),
                person.getLastName(),
                person.fullName(),
                person.getPhone(),
                person.getGender(),
                person.getBirthDate(),
                person.getContactEmail(),
                person.getAddress(),
                person.getDistrict(),
                documentTypeCode(document),
                document != null ? document.getDocumentNumber() : null,
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone(),
                Boolean.TRUE.equals(person.getProfileCompleted()),
                account != null,
                account != null ? account.getEmail() : null,
                account != null ? account.getIsActive() : null,
                PortalAccountStatus.of(account),
                !Boolean.FALSE.equals(patient.getIsActive()),
                patient.getCreatedAt());
    }

    public static PatientProfileResponse toProfile(User account, Patient patient) {
        Person person = account.getPerson();
        PersonDocument document = person.primaryDocument();
        return new PatientProfileResponse(
                person.getId(),
                patient != null ? patient.getId() : null,
                patient != null ? patient.getMedicalRecordNumber() : null,
                person.getFirstName(),
                person.getLastName(),
                person.getPhone(),
                person.getGender(),
                person.getBirthDate(),
                person.getContactEmail(),
                person.getAddress(),
                person.getDistrict(),
                documentTypeCode(document),
                document != null ? document.getDocumentNumber() : null,
                patient != null ? patient.getEmergencyContactName() : null,
                patient != null ? patient.getEmergencyContactPhone() : null,
                Boolean.TRUE.equals(person.getProfileCompleted()),
                true,
                Boolean.TRUE.equals(account.getIsActive()));
    }

    private static String documentTypeCode(PersonDocument document) {
        if (document == null || document.getDocumentType() == null) {
            return null;
        }
        return document.getDocumentType().getCode();
    }
}
