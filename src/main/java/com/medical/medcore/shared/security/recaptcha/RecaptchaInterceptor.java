package com.medical.medcore.shared.security.recaptcha;

import com.medical.medcore.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RecaptchaInterceptor implements HandlerInterceptor {

    public static final String TOKEN_HEADER = "X-Recaptcha-Token";

    static final Map<String, String> ACTIONS = Map.of(
            "/auth/login", "login",
            "/auth/register", "register",
            "/auth/forgot-password", "forgot_password",
            "/auth/reset-password", "reset_password",
            "/public/patients/register", "patient_register",
            "/public/bookings", "booking",
            "/auth/activate", "activate_account",
            "/public/email-verification", "email_verification");

    private final RecaptchaVerifier verifier;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!verifier.isEnabled() || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String action = ACTIONS.get(request.getServletPath());
        if (action != null) {
            verifier.verify(request.getHeader(TOKEN_HEADER), action, ClientIpResolver.resolve(request));
        }
        return true;
    }
}
