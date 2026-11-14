package com.medical.medcore.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
                .birthDate(LocalDate.of(1990, 1, 1)).contactEmail("ana@test.com")
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
}
