package com.medical.medcore.patient.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.identity.service.PersonData;
import com.medical.medcore.identity.service.PersonRegistry;
import com.medical.medcore.identity.service.TokenIssuer;
import com.medical.medcore.identity.service.UserAccounts;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.dto.PatientResponse;
import com.medical.medcore.patient.dto.PatientSelfRegistrationRequest;
import com.medical.medcore.patient.dto.QuickPatientRegistrationRequest;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.security.RoleConstants;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PatientRegistrationService {

    private static final String CODE_DOCUMENT_TAKEN = "DOCUMENT_ALREADY_REGISTERED";
    private static final String CODE_IDENTITY_UNVERIFIED = "IDENTITY_NOT_VERIFIED";

    private final PersonRegistry personRegistry;
    private final UserAccounts userAccounts;
    private final UserRepository userRepository;
    private final PatientEnrollment patientEnrollment;
    private final TokenIssuer tokenIssuer;

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        String normalized = PersonRegistry.normalizeEmail(email);
        return normalized != null
                && !userRepository.existsByEmailAndTenantId(normalized, TenantContext.requireTenantId());
    }

    @Transactional
    public PatientResponse registerQuick(QuickPatientRegistrationRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String email = userAccounts.requireEmail(request.email());
        userAccounts.assertEmailAvailable(email, tenantId);
        PersonData data = new PersonData(request.firstName(), request.lastName(), null, null, request.phone(),
                email, null, null, request.documentTypeCode(), request.documentNumber());

        personRegistry.findByDocument(tenantId, data.documentTypeCode(), data.documentNumber())
                .ifPresent(existing -> {
                    throw documentTaken(existing, tenantId);
                });

        Person person = personRegistry.create(tenantId, data);
        User account = openPatientAccount(tenantId, person, email, request.password());
        Patient patient = patientEnrollment.ensurePatient(tenantId, person);
        return PatientMapper.toResponse(patient, account);
    }

    @Transactional
    public AuthResponse registerSelf(PatientSelfRegistrationRequest request) {
        return register(request).session();
    }

    @Transactional
    public Registration register(PatientSelfRegistrationRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String email = userAccounts.requireEmail(request.email());
        userAccounts.assertEmailAvailable(email, tenantId);
        PersonData data = new PersonData(request.firstName(), request.lastName(), request.birthDate(), null,
                request.phone(), email, null, null, request.documentTypeCode(), request.documentNumber());

        Person person = personRegistry.findByDocument(tenantId, data.documentTypeCode(), data.documentNumber())
                .map(existing -> claimExistingPerson(existing, tenantId, request.birthDate(), email))
                .orElseGet(() -> personRegistry.create(tenantId, data));

        User account = openPatientAccount(tenantId, person, email, request.password());
        Patient patient = patientEnrollment.ensurePatient(tenantId, person);
        return new Registration(patient, tokenIssuer.issue(account));
    }

    public record Registration(Patient patient, AuthResponse session) {
    }

    private Person claimExistingPerson(Person existing, Long tenantId, LocalDate declaredBirthDate, String email) {
        if (userRepository.existsByPersonIdAndTenantId(existing.getId(), tenantId)) {
            throw new ConflictException(CODE_DOCUMENT_TAKEN, "Este documento ya tiene una cuenta. Inicie sesión");
        }
        boolean verified = existing.getBirthDate() != null && existing.getBirthDate().equals(declaredBirthDate);
        if (!verified) {
            throw new ConflictException(CODE_IDENTITY_UNVERIFIED,
                    "Tu documento ya está registrado en la clínica. Acércate a recepción para activar tu cuenta");
        }
        if (existing.getContactEmail() == null) {
            existing.setContactEmail(email);
            existing.refreshProfileCompleted();
        }
        return existing;
    }

    private ConflictException documentTaken(Person existing, Long tenantId) {
        if (userRepository.existsByPersonIdAndTenantId(existing.getId(), tenantId)) {
            return new ConflictException(CODE_DOCUMENT_TAKEN, "Este documento ya tiene una cuenta. Inicie sesión");
        }
        return new ConflictException(CODE_IDENTITY_UNVERIFIED,
                "Tu documento ya está registrado en la clínica. Acércate a recepción para activar tu cuenta");
    }

    private User openPatientAccount(Long tenantId, Person person, String email, String password) {
        return userAccounts.create(tenantId, person, email, password,
                Set.of(userAccounts.requireRole(RoleConstants.PATIENT)));
    }
}
