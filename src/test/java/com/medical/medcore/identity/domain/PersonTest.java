package com.medical.medcore.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PersonTest {

    @Test
    void buildsTheFullNameIgnoringMissingParts() {
        assertThat(Person.builder().firstName(" Ana ").lastName("Rivas").build().fullName()).isEqualTo("Ana Rivas");
        assertThat(Person.builder().firstName("Ana").build().fullName()).isEqualTo("Ana");
        assertThat(Person.builder().build().fullName()).isNull();
    }

    @Test
    void theProfileIsCompleteOnlyWithContactDataAndADocument() {
        Person person = Person.builder()
                .firstName("Ana").lastName("Rivas").phone("999").gender("F")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        person.refreshProfileCompleted();
        assertThat(person.getProfileCompleted()).isFalse();

        person.getDocuments().add(PersonDocument.builder().person(person).documentNumber("12345678").build());
        person.refreshProfileCompleted();
        assertThat(person.getProfileCompleted()).isTrue();

        person.setPhone(" ");
        person.refreshProfileCompleted();
        assertThat(person.getProfileCompleted()).isFalse();
    }

    @Test
    void listsTheMissingFieldsInDisplayOrder() {
        Person person = Person.builder().firstName("Walter").lastName("Paz").build();

        assertThat(person.missingFields(ProfileField.patientFields()))
                .containsExactly(ProfileField.DOCUMENT, ProfileField.BIRTH_DATE, ProfileField.GENDER, ProfileField.PHONE);
        assertThat(person.missingFields(ProfileField.requiredFor(List.of("RECEPTIONIST"))))
                .containsExactly(ProfileField.DOCUMENT, ProfileField.GENDER, ProfileField.PHONE);
    }

    @Test
    void patientsMustAlsoGiveTheirBirthDate() {
        assertThat(ProfileField.requiredFor(List.of("PATIENT"))).contains(ProfileField.BIRTH_DATE);
        assertThat(ProfileField.requiredFor(List.of("DOCTOR", "PATIENT"))).contains(ProfileField.BIRTH_DATE);
        assertThat(ProfileField.requiredFor(Set.of("DOCTOR"))).doesNotContain(ProfileField.BIRTH_DATE);
        assertThat(ProfileField.describe(List.of(ProfileField.BIRTH_DATE, ProfileField.PHONE)))
                .isEqualTo("fecha de nacimiento, teléfono");
    }

    @Test
    void theIdentityCheckLockExpires() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 1, 8, 0);
        Person person = Person.builder().identityCheckLockedUntil(now.plusMinutes(15)).build();

        assertThat(person.isIdentityCheckLockedAt(now)).isTrue();
        assertThat(person.isIdentityCheckLockedAt(now.plusMinutes(16))).isFalse();
        assertThat(Person.builder().build().isIdentityCheckLockedAt(now)).isFalse();
    }
}
