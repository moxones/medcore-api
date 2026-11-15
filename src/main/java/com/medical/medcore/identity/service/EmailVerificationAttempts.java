package com.medical.medcore.identity.service;

import com.medical.medcore.identity.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuenta los intentos fallidos en su propia transacción: el registro que los provoca se revierte por el error, pero
 * el intento debe quedar contado para que nadie pueda probar códigos sin límite.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationAttempts {

    private final EmailVerificationRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long verificationId) {
        repository.registerFailedAttempt(verificationId);
    }
}
