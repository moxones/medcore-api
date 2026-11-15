package com.medical.medcore.identity.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.medical.medcore.identity.domain.ProfileField;
import com.medical.medcore.identity.domain.Role;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.shared.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * Impide usar la aplicación mientras la cuenta tenga pendiente el primer ingreso: cambiar la clave temporal y
 * completar los datos obligatorios ({@link ProfileField}). Solo se recuerdan las cuentas que ya están al día, así
 * que completar algo se refleja en la petición siguiente; lo contrario (un admin resetea la clave) invalida el
 * recuerdo con {@link #forget(Long, Long)}.
 */
@Service
public class OnboardingGate {

    public static final String CODE_PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";
    public static final String CODE_PROFILE_COMPLETION_REQUIRED = "PROFILE_COMPLETION_REQUIRED";

    private final UserRepository userRepository;
    private final Cache<String, Boolean> cleared;

    public OnboardingGate(UserRepository userRepository,
                          @Value("${app.auth.onboarding-cache-seconds:30}") long cacheSeconds) {
        this.userRepository = userRepository;
        this.cleared = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(cacheSeconds))
                .maximumSize(50_000)
                .build();
    }

    @Transactional(readOnly = true)
    public void assertCleared(Long tenantId, Long userId) {
        String key = key(tenantId, userId);
        if (cleared.getIfPresent(key) != null) {
            return;
        }
        User user = userRepository.findByIdAndTenantId(userId, tenantId).orElse(null);
        if (user == null) {
            return;
        }
        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            throw new ForbiddenException(CODE_PASSWORD_CHANGE_REQUIRED,
                    "Debes cambiar tu contraseña temporal antes de continuar");
        }
        List<String> roleCodes = user.getRoles().stream().map(Role::getCode).toList();
        List<ProfileField> missing = user.getPerson().missingFields(ProfileField.requiredFor(roleCodes));
        if (!missing.isEmpty()) {
            throw new ForbiddenException(CODE_PROFILE_COMPLETION_REQUIRED,
                    "Completa tu perfil para continuar. Falta: " + ProfileField.describe(missing));
        }
        cleared.put(key, Boolean.TRUE);
    }

    public void forget(Long tenantId, Long userId) {
        cleared.invalidate(key(tenantId, userId));
    }

    private static String key(Long tenantId, Long userId) {
        return tenantId + ":" + userId;
    }
}
