package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Encuentra la cuenta de una organización por lo que la persona recuerde: su correo (la cuenta) o su número de
 * documento (la ficha). Si el número coincide con dos cuentas (p. ej. un DNI y un carné con el mismo número) no se
 * adivina: no hay coincidencia y la persona debe usar su correo.
 */
@Service
@RequiredArgsConstructor
public class AccountLookup {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<User> find(Long tenantId, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String value = identifier.trim();
        if (value.contains("@")) {
            return userRepository.findByEmailAndTenantId(PersonRegistry.normalizeEmail(value), tenantId);
        }
        List<User> matches = userRepository.findByDocumentNumber(tenantId, value.replaceAll("\\s", ""));
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }
}
