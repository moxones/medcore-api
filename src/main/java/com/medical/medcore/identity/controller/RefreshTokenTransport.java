package com.medical.medcore.identity.controller;

import com.medical.medcore.identity.dto.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshTokenTransport {

    public static final String TRANSPORT_HEADER = "X-Refresh-Transport";
    private static final String COOKIE_TRANSPORT = "cookie";
    private static final String COOKIE_NAME = "medcore_refresh";
    private static final String AUTH_PATH = "/auth";

    private final boolean secure;
    private final Duration maxAge;

    public RefreshTokenTransport(@Value("${app.auth.refresh-cookie-secure:true}") boolean secure,
                                 @Value("${security.jwt.refresh-expiration:604800000}") long refreshExpirationMs) {
        this.secure = secure;
        this.maxAge = Duration.ofMillis(refreshExpirationMs);
    }

    public AuthResponse deliver(AuthResponse session, HttpServletRequest request, HttpServletResponse response) {
        if (!usesCookie(request)) {
            return session;
        }
        writeCookie(response, request, session.refreshToken(), maxAge);
        return session.withoutRefreshToken();
    }

    public String read(String bodyToken, HttpServletRequest request) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return bodyToken;
        }
        return Optional.ofNullable(request.getCookies()).stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        writeCookie(response, request, "", Duration.ZERO);
    }

    private boolean usesCookie(HttpServletRequest request) {
        return COOKIE_TRANSPORT.equalsIgnoreCase(request.getHeader(TRANSPORT_HEADER));
    }

    private void writeCookie(HttpServletResponse response, HttpServletRequest request, String value, Duration age) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(request.getContextPath() + AUTH_PATH)
                .maxAge(age)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
