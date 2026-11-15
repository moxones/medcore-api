package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.PersonDocument;
import com.medical.medcore.identity.domain.Role;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.InvitationResponse;
import com.medical.medcore.identity.dto.PersonRequest;
import com.medical.medcore.identity.dto.PersonResponse;
import com.medical.medcore.identity.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return toResponse(user, null);
    }

    public static UserResponse toResponse(User user, InvitationResponse invitation) {
        List<Role> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().sorted(Comparator.comparing(Role::getId)).toList();
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getIsActive(),
                toPersonResponse(user.getPerson()),
                roles.stream().map(Role::getCode).toList(),
                roles.stream().map(Role::getId).toList(),
                user.getTenantId(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.isLockedAt(LocalDateTime.now()),
                user.isPendingActivation(),
                invitation);
    }

    public static PersonResponse toPersonResponse(Person person) {
        if (person == null) {
            return null;
        }
        PersonDocument document = person.primaryDocument();
        String documentTypeCode = document != null && document.getDocumentType() != null
                ? document.getDocumentType().getCode()
                : null;
        return new PersonResponse(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getBirthDate(),
                person.getGender(),
                person.getPhone(),
                person.getContactEmail(),
                person.getAddress(),
                person.getDistrict(),
                Boolean.TRUE.equals(person.getProfileCompleted()),
                documentTypeCode,
                document != null ? document.getDocumentNumber() : null);
    }

    public static PersonData toPersonData(PersonRequest request, String fallbackEmail) {
        if (request == null) {
            return new PersonData(null, null, null, null, null, fallbackEmail, null, null, null, null);
        }
        return new PersonData(
                request.firstName(),
                request.lastName(),
                request.birthDate(),
                request.gender(),
                request.phone(),
                request.contactEmail() != null ? request.contactEmail() : fallbackEmail,
                request.address(),
                request.district(),
                request.documentTypeCode(),
                request.documentNumber());
    }
}
