package com.medical.medcore.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String CODE_RATE_LIMITED = "RATE_LIMITED";
    private static final Set<String> CREDENTIAL_PATHS = Set.of(
            "/auth/login", "/auth/register", "/auth/forgot-password", "/auth/reset-password",
            "/public/patients/register", "/auth/activation/check", "/auth/activate",
            "/public/email-verification");

    private final Cache<String, WindowCounter> counters;
    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final int maxAuthRequests;
    private final long windowMs;

    public RateLimitInterceptor(ObjectMapper objectMapper,
                                @Value("${app.rate-limit.max-requests:300}") int maxRequests,
                                @Value("${app.rate-limit.auth-max-requests:30}") int maxAuthRequests,
                                @Value("${app.rate-limit.window-ms:60000}") long windowMs) {
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.maxAuthRequests = maxAuthRequests;
        this.windowMs = windowMs;
        this.counters = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMillis(windowMs * 2))
                .maximumSize(100_000)
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        boolean authPath = CREDENTIAL_PATHS.contains(request.getServletPath());
        String key = clientKey(request, authPath);
        int limit = authPath ? maxAuthRequests : maxRequests;
        if (counters.get(key, ignored -> new WindowCounter(windowMs)).tryConsume(limit)) {
            return true;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(Math.max(1, windowMs / 1000)));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("Demasiadas solicitudes, intente nuevamente en un momento", CODE_RATE_LIMITED));
        return false;
    }

    private String clientKey(HttpServletRequest request, boolean authPath) {
        Long userId = TenantContext.getCurrentUserId();
        if (authPath || userId == null) {
            return (authPath ? "auth:" : "ip:") + ClientIpResolver.resolve(request);
        }
        return "user:" + TenantContext.getTenantId() + ":" + userId;
    }

    private static final class WindowCounter {

        private final long windowMs;
        private long windowStart;
        private int used;

        private WindowCounter(long windowMs) {
            this.windowMs = windowMs;
            this.windowStart = System.currentTimeMillis();
        }

        private synchronized boolean tryConsume(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMs) {
                windowStart = now;
                used = 0;
            }
            if (used >= limit) {
                return false;
            }
            used++;
            return true;
        }
    }
}
