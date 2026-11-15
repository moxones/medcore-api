package com.medical.medcore.identity.service;

import com.medical.medcore.identity.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class IdentityCheckRecorder {

    private final PersonRepository personRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long personId, int maxAttempts, LocalDateTime lockUntil) {
        personRepository.registerIdentityCheckFailure(personId, maxAttempts, lockUntil);
    }
}
