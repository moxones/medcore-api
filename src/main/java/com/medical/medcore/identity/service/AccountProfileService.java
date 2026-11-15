package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.PersonDocument;
import com.medical.medcore.identity.domain.ProfileField;
import com.medical.medcore.identity.domain.Role;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.AccountProfileResponse;
import com.medical.medcore.identity.dto.CompleteProfileRequest;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountProfileService {

    private static final String CODE_PROFILE_INCOMPLETE = "PROFILE_INCOMPLETE";
    private static final String CODE_DOCUMENT_LOCKED = "DOCUMENT_LOCKED";

    private final UserRepository userRepository;
    private final PersonRegistry personRegistry;

    public AccountProfileResponse current() {
        return toResponse(currentUser());
    }

    @Transactional
    public AccountProfileResponse complete(CompleteProfileRequest request) {
        User user = currentUser();
        Person person = user.getPerson();
        boolean documentProvided = hasText(request.documentTypeCode()) || hasText(request.documentNumber());
        boolean setsDocument = documentProvided && person.primaryDocument() == null;
        if (documentProvided && !setsDocument) {
            assertSameDocument(person.primaryDocument(), request);
        }
        String contactEmail = request.contactEmail() != null || person.getContactEmail() != null
                ? request.contactEmail()
                : user.getEmail();
        personRegistry.applyChanges(person, new PersonData(
                request.firstName(), request.lastName(), request.birthDate(), request.gender(), request.phone(),
                contactEmail, request.address(), request.district(),
                setsDocument ? request.documentTypeCode() : null,
                setsDocument ? request.documentNumber() : null));
        List<ProfileField> missing = person.missingFields(requiredFields(user));
        if (!missing.isEmpty()) {
            throw new BadRequestException(CODE_PROFILE_INCOMPLETE,
                    "Faltan datos obligatorios: " + ProfileField.describe(missing));
        }
        return toResponse(user);
    }

    private void assertSameDocument(PersonDocument current, CompleteProfileRequest request) {
        String type = request.documentTypeCode() == null ? "" : request.documentTypeCode().trim().toUpperCase();
        String number = request.documentNumber() == null ? "" : request.documentNumber().trim().toUpperCase();
        boolean same = current.getDocumentType().getCode().equals(type) && current.getDocumentNumber().equals(number);
        if (!same) {
            throw new ConflictException(CODE_DOCUMENT_LOCKED,
                    "Tu documento ya está registrado. Para corregirlo acércate a recepción");
        }
    }

    private AccountProfileResponse toResponse(User user) {
        Person person = user.getPerson();
        PersonDocument document = person.primaryDocument();
        Set<ProfileField> required = requiredFields(user);
        List<ProfileField> missing = person.missingFields(required);
        return new AccountProfileResponse(
                user.getEmail(),
                person.getFirstName(),
                person.getLastName(),
                document != null ? document.getDocumentType().getCode() : null,
                document != null ? document.getDocumentNumber() : null,
                person.getBirthDate(),
                person.getGender(),
                person.getPhone(),
                person.getContactEmail(),
                person.getAddress(),
                person.getDistrict(),
                required.stream().sorted().toList(),
                missing,
                missing.isEmpty());
    }

    private Set<ProfileField> requiredFields(User user) {
        return ProfileField.requiredFor(user.getRoles().stream().map(Role::getCode).toList());
    }

    private User currentUser() {
        User user = userRepository.findByIdAndTenantId(TenantContext.requireCurrentUserId(), TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ForbiddenException("ACCOUNT_INACTIVE", "Usuario inactivo");
        }
        return user;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
