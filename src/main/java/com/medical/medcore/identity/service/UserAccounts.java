package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.Role;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.repository.PasswordResetTokenRepository;
import com.medical.medcore.identity.repository.RoleRepository;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserAccounts {

    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final String CODE_EMAIL_TAKEN = "EMAIL_ALREADY_REGISTERED";
    private static final String CODE_ACCOUNT_EXISTS = "ACCOUNT_ALREADY_EXISTS";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User create(Long tenantId, Person person, String email, String rawPassword, Set<Role> roles) {
        String normalizedEmail = requireEmail(email);
        assertEmailAvailable(normalizedEmail, tenantId);
        if (userRepository.existsByPersonIdAndTenantId(person.getId(), tenantId)) {
            throw new ConflictException(CODE_ACCOUNT_EXISTS, "Esta persona ya tiene una cuenta de acceso");
        }
        if (roles == null || roles.isEmpty()) {
            throw new BadRequestException("Debe asignar al menos un rol");
        }
        User user = User.builder()
                .tenantId(tenantId)
                .person(person)
                .email(normalizedEmail)
                .password(encode(rawPassword))
                .passwordChangedAt(LocalDateTime.now())
                .roles(new HashSet<>(roles))
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User createPendingActivation(Long tenantId, Person person, String email, Set<Role> roles) {
        User user = create(tenantId, person, email, TokenHasher.newOpaqueToken(), roles);
        user.setPendingActivation(true);
        user.setPasswordChangedAt(null);
        return userRepository.save(user);
    }

    @Transactional
    public User claimPending(User pending, String email, String rawPassword) {
        String normalizedEmail = requireEmail(email);
        if (!normalizedEmail.equals(pending.getEmail())) {
            assertEmailAvailable(normalizedEmail, pending.getTenantId());
            pending.setEmail(normalizedEmail);
        }
        pending.changePassword(encode(rawPassword), LocalDateTime.now(), false);
        tokenRepository.invalidatePending(pending.getId(), LocalDateTime.now());
        return userRepository.save(pending);
    }

    @Transactional(readOnly = true)
    public Role requireRole(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Rol no configurado: " + code));
    }

    public void assertEmailAvailable(String normalizedEmail, Long tenantId) {
        if (userRepository.existsByEmailAndTenantId(normalizedEmail, tenantId)) {
            throw new ConflictException(CODE_EMAIL_TAKEN, "El email ya se encuentra registrado");
        }
    }

    public String requireEmail(String email) {
        String normalized = PersonRegistry.normalizeEmail(email);
        if (normalized == null) {
            throw new BadRequestException("El email es obligatorio");
        }
        return normalized;
    }

    public String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new BadRequestException("La contraseña no puede superar los " + MAX_PASSWORD_LENGTH + " caracteres");
        }
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, User user) {
        return rawPassword != null && passwordEncoder.matches(rawPassword, user.getPassword());
    }
}
