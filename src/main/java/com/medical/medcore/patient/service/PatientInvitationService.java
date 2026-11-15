package com.medical.medcore.patient.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.identity.service.AccountActivationService;
import com.medical.medcore.identity.service.PersonRegistry;
import com.medical.medcore.identity.service.UserAccounts;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.dto.PortalInvitationResponse;
import com.medical.medcore.patient.repository.PatientRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.RoleConstants;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PatientInvitationService {

    private static final String CODE_ACCOUNT_EXISTS = "ACCOUNT_ALREADY_EXISTS";
    private static final String CODE_EMAIL_REQUIRED = "EMAIL_REQUIRED";
    private static final String CODE_PATIENT_INACTIVE = "PATIENT_INACTIVE";

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final UserAccounts userAccounts;
    private final AccountActivationService activationService;

    @Transactional
    public PortalInvitationResponse invite(Long patientId, String requestedEmail, String requesterIp) {
        Long tenantId = TenantContext.requireTenantId();
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));
        if (Boolean.FALSE.equals(patient.getIsActive())) {
            throw new ConflictException(CODE_PATIENT_INACTIVE, "El paciente está inactivo");
        }
        Person person = patient.getPerson();
        Optional<User> existing = userRepository.findByPersonIdAndTenantId(person.getId(), tenantId);
        if (existing.isPresent() && !existing.get().isPendingActivation()) {
            throw new ConflictException(CODE_ACCOUNT_EXISTS, "El paciente ya tiene una cuenta activa en el portal");
        }
        String email = resolveEmail(requestedEmail, existing.map(User::getEmail).orElse(person.getContactEmail()));
        User account = existing
                .map(pending -> updateEmail(pending, email))
                .orElseGet(() -> userAccounts.createPendingActivation(tenantId, person, email,
                        Set.of(userAccounts.requireRole(RoleConstants.PATIENT))));
        if (person.getContactEmail() == null) {
            person.setContactEmail(email);
        }
        AccountActivationService.ActivationTicket ticket = activationService.issue(account, requesterIp);
        return new PortalInvitationResponse(patient.getId(), account.getEmail(), ticket.expiresAt(),
                existing.isPresent(), ticket.delivered(), ticket.token());
    }

    private String resolveEmail(String requested, String fallback) {
        String email = PersonRegistry.normalizeEmail(requested != null ? requested : fallback);
        if (email == null) {
            throw new BadRequestException(CODE_EMAIL_REQUIRED,
                    "Registra un correo del paciente para enviarle la invitación");
        }
        return email;
    }

    private User updateEmail(User pending, String email) {
        if (!email.equals(pending.getEmail())) {
            userAccounts.assertEmailAvailable(email, pending.getTenantId());
            pending.setEmail(email);
        }
        return userRepository.save(pending);
    }
}
