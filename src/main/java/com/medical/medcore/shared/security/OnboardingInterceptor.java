package com.medical.medcore.shared.security;

import com.medical.medcore.identity.service.OnboardingGate;
import com.medical.medcore.shared.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Mientras la cuenta tenga pendiente el primer ingreso solo deja pasar lo necesario para resolverlo: la sesión
 * ({@code /auth/*}), el perfil propio (también el del portal del paciente), el cambio de clave y lo público ({@code /public/*}), aunque llegue con token.
 */
@Component
@RequiredArgsConstructor
public class OnboardingInterceptor implements HandlerInterceptor {

    public static final Set<String> OPEN_PATHS = Set.of("/profile", "/profile/password", "/patients/profile");
    private static final String AUTH_PREFIX = "/auth/";
    private static final String PUBLIC_PREFIX = "/public/";

    private final OnboardingGate gate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = TenantContext.getCurrentUserId();
        if (userId == null || isOpen(request.getServletPath())) {
            return true;
        }
        gate.assertCleared(TenantContext.requireTenantId(), userId);
        return true;
    }

    private static boolean isOpen(String path) {
        return path.startsWith(AUTH_PREFIX) || path.startsWith(PUBLIC_PREFIX) || OPEN_PATHS.contains(path);
    }
}
