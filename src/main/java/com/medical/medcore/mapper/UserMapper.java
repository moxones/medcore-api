package com.medical.medcore.mapper;

import com.medical.medcore.entity.User;
import com.medical.medcore.entity.Role;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.PersonDocument;
import com.medical.medcore.dto.response.UserResponse;
import com.medical.medcore.dto.response.PersonResponse;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {

        Person personEntity = user.getPerson();
        boolean hidePersonData = personEntity != null
                && personEntity.getTenantId() != null
                && !Objects.equals(personEntity.getTenantId(), user.getTenantId());

        String documentTypeCode = null;
        String documentNumber = null;

        if (!hidePersonData && personEntity != null && personEntity.getDocuments() != null && !personEntity.getDocuments().isEmpty()) {
            PersonDocument doc = personEntity.getDocuments().get(0);
            if (doc.getDocumentType() != null) {
                documentTypeCode = doc.getDocumentType().getCode();
            }
            documentNumber = doc.getDocumentNumber();
        }

        PersonResponse person = personEntity == null
                ? null
                : hidePersonData
                ? new PersonResponse(null, null, null, null, null, null, null)
                : new PersonResponse(
                        personEntity.getFirstName(),
                        personEntity.getLastName(),
                        personEntity.getBirthDate(),
                        personEntity.getGender(),
                        personEntity.getPhone(),
                        documentTypeCode,
                        documentNumber
                );

        List<Role> userRoles = user.getRoles() == null
                ? Collections.emptyList()
                : user.getRoles().stream().toList();

        List<String> roles = userRoles
                .stream()
                .map(Role::getCode)
                .toList();
                
        List<Long> roleIds = userRoles
                .stream()
                .map(Role::getId)
                .toList();

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getIsActive(),
                person,
                roles,
                roleIds,
                user.getTenantId()
        );
    }
}
