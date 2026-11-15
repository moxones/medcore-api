package com.medical.medcore.patient.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.identity.service.EmailVerificationService;
import com.medical.medcore.identity.service.IdentityVerifier;
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

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PatientRegistrationService {

    private static final String CODE_DOCUMENT_TAKEN = "DOCUMENT_ALREADY_REGISTERED";

    private final PersonRegistry personRegistry;
    private final UserAccounts userAccounts;
    private final UserRepository userRepository;
    private final PatientEnrollment patientEnrollment;
    private final TokenIssuer tokenIssuer;
    private final IdentityVerifier identityVerifier;
    private final EmailVerificationService emailVerification;

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        String normalized = PersonRegistry.normalizeEmail(email);
        return normalized != null
                && !userRepository.existsByEmailAndTenantId(normalized, TenantContext.requireTenantId());
    }

    @Transactional
    public PatientResponse registerQuick(QuickPatientRegistrationRequest request) {
        PersonData data = new PersonData(request.firstName(), request.lastName(), request.birthDate(), null,
                request.phone(), request.email(), null, null, request.documentTypeCode(), request.documentNumber());
        Enrollment enrollment = enroll(data, request.email(), request.password(), request.emailCode());
        return PatientMapper.toResponse(enrollment.patient(), enrollment.account());
    }

    @Transactional
    public AuthResponse registerSelf(PatientSelfRegistrationRequest request) {
        return register(request).session();
    }

    @Transactional
    public Registration register(PatientSelfRegistrationRequest request) {
        PersonData data = new PersonData(request.firstName(), request.lastName(), request.birthDate(), request.gender(),
                request.phone(), request.email(), null, null, request.documentTypeCode(), request.documentNumber());
        Enrollment enrollment = enroll(data, request.email(), request.password(), request.emailCode());
        return new Registration(enrollment.patient(), tokenIssuer.issue(enrollment.account()));
    }

    public record Registration(Patient patient, AuthResponse session) {
    }

    private record Enrollment(Patient patient, User account) {
    }

    private Enrollment enroll(PersonData data, String rawEmail, String password, String emailCode) {
        Long tenantId = TenantContext.requireTenantId();
        String email = userAccounts.requireEmail(rawEmail);
        emailVerification.consume(email, emailCode);
        Optional<Person> existing = personRegistry.findByDocument(tenantId, data.documentTypeCode(), data.documentNumber());
        if (existing.isEmpty()) {
            userAccounts.assertEmailAvailable(email, tenantId);
            Person person = personRegistry.create(tenantId, withEmail(data, email));
            User account = openPatientAccount(tenantId, person, email, password);
            return new Enrollment(patientEnrollment.ensurePatient(tenantId, person), account);
        }
        Person person = existing.get();
        Optional<User> pendingAccount = userRepository.findByPersonIdAndTenantId(person.getId(), tenantId);
        if (pendingAccount.isPresent() && !pendingAccount.get().isPendingActivation()) {
            throw new ConflictException(CODE_DOCUMENT_TAKEN,
                    "Este documento ya tiene una cuenta. Inicia sesión o recupera tu contraseña");
        }
        identityVerifier.verify(person, data.birthDate());
        if (person.getContactEmail() == null) {
            person.setContactEmail(email);
        }
        person.refreshProfileCompleted();
        User account = pendingAccount
                .map(pending -> userAccounts.claimPending(pending, email, password))
                .orElseGet(() -> {
                    userAccounts.assertEmailAvailable(email, tenantId);
                    return openPatientAccount(tenantId, person, email, password);
                });
        return new Enrollment(patientEnrollment.ensurePatient(tenantId, person), account);
    }

    private PersonData withEmail(PersonData data, String email) {
        return new PersonData(data.firstName(), data.lastName(), data.birthDate(), data.gender(), data.phone(), email,
                data.address(), data.district(), data.documentTypeCode(), data.documentNumber());
    }

    private User openPatientAccount(Long tenantId, Person person, String email, String password) {
        return userAccounts.create(tenantId, person, email, password,
                Set.of(userAccounts.requireRole(RoleConstants.PATIENT)));
    }
}
