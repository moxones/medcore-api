package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IdentityVerifier {

    public static final String CODE_VERIFICATION_REQUIRED = "IDENTITY_VERIFICATION_REQUIRED";
    public static final String CODE_NOT_VERIFIED = "IDENTITY_NOT_VERIFIED";
    public static final String CODE_CHECK_LOCKED = "IDENTITY_CHECK_LOCKED";

    private final IdentityCheckRecorder recorder;

    @Value("${app.auth.max-failed-attempts:5}")
    private int maxAttempts;

    @Value("${app.auth.lock-minutes:15}")
    private int lockMinutes;

    public void verify(Person person, LocalDate declaredBirthDate) {
        LocalDateTime now = LocalDateTime.now();
        if (person.isIdentityCheckLockedAt(now)) {
            throw new TooManyRequestsException(CODE_CHECK_LOCKED,
                    "Demasiados intentos de verificación. Intenta más tarde o acércate a recepción");
        }
        if (declaredBirthDate == null) {
            throw new ConflictException(CODE_VERIFICATION_REQUIRED,
                    "Ya tienes una ficha en la clínica. Confirma tu fecha de nacimiento para vincular tu cuenta");
        }
        if (!declaredBirthDate.equals(person.getBirthDate())) {
            recorder.recordFailure(person.getId(), maxAttempts, now.plusMinutes(lockMinutes));
            throw new ConflictException(CODE_NOT_VERIFIED,
                    "No pudimos verificar tu identidad. Revisa la fecha o pide en recepción una invitación al portal");
        }
        person.setIdentityCheckFailures(0);
        person.setIdentityCheckLockedUntil(null);
    }
}
