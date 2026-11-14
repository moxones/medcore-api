package com.medical.medcore.identity.service;

import com.medical.medcore.catalog.domain.DocumentType;
import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.PersonDocument;
import com.medical.medcore.identity.domain.Role;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void mapsUserWithoutRolesOrPerson() {
        User user = new User();
        user.setId(1L);
        user.setEmail("superadmin@test.com");
        user.setIsActive(true);
        user.setRoles(null);

        UserResponse response = UserMapper.toResponse(user);

        assertThat(response.roles()).isEmpty();
        assertThat(response.roleIds()).isEmpty();
        assertThat(response.person()).isNull();
        assertThat(response.locked()).isFalse();
    }

    @Test
    void mapsPersonDocumentAndSortedRoles() {
        Person person = Person.builder().id(7L).tenantId(2L).firstName("Ana").lastName("Rivas").build();
        person.getDocuments().add(PersonDocument.builder()
                .person(person)
                .documentType(DocumentType.builder().code("DNI").name("DNI").build())
                .documentNumber("12345678")
                .build());
        User user = User.builder()
                .id(3L)
                .tenantId(2L)
                .email("ana@clinic.com")
                .person(person)
                .roles(Set.of(new Role(5L, "Receptionist", "RECEPTIONIST"), new Role(3L, "Doctor", "DOCTOR")))
                .build();

        UserResponse response = UserMapper.toResponse(user);

        assertThat(response.roles()).isEqualTo(List.of("DOCTOR", "RECEPTIONIST"));
        assertThat(response.roleIds()).isEqualTo(List.of(3L, 5L));
        assertThat(response.person().documentTypeCode()).isEqualTo("DNI");
        assertThat(response.person().documentNumber()).isEqualTo("12345678");
    }

    @Test
    void flagsLockedAccounts() {
        User user = User.builder().id(9L).email("locked@clinic.com").build();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        assertThat(UserMapper.toResponse(user).locked()).isTrue();
    }
}
