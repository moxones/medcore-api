package com.medical.medcore.mapper;

import com.medical.medcore.dto.response.UserResponse;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMapperTest {

    @Test
    void toResponse_handlesNullRolesAndPerson() {
        User user = new User();
        user.setId(1L);
        user.setEmail("superadmin@test.com");
        user.setIsActive(true);

        UserResponse response = assertDoesNotThrow(() -> UserMapper.toResponse(user));

        assertNotNull(response);
        assertTrue(response.getRoles().isEmpty());
        assertTrue(response.getRoleIds().isEmpty());
    }

    @Test
    void toResponse_hidesPersonDataWhenPersonBelongsToAnotherTenant() {
        Person person = new Person();
        person.setTenantId(1L);
        person.setFirstName("Ana");
        person.setLastName("Perez");

        User user = new User();
        user.setId(2L);
        user.setEmail("tenant-b@test.com");
        user.setIsActive(true);
        user.setTenantId(2L);
        user.setPerson(person);

        UserResponse response = UserMapper.toResponse(user);

        assertNotNull(response);
        assertNotNull(response.getPerson());
        assertNull(response.getPerson().getFirstName());
        assertNull(response.getPerson().getLastName());
        assertNull(response.getPerson().getDocumentTypeCode());
        assertNull(response.getPerson().getDocumentNumber());
    }
}
