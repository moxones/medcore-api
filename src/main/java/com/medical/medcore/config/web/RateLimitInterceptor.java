package com.medical.medcore.config.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS = 300;
    private static final long REFILL_DURATION_MS = 60000;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String clientIp = resolveClientIp(request);

        if (buckets.size() > MAX_TRACKED_CLIENTS) {
            buckets.clear();
        }

        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(MAX_REQUESTS, REFILL_DURATION_MS));

        if (bucket.tryConsume()) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"data\":null,\"message\":\"Demasiadas solicitudes, intente nuevamente en un momento\"}");
        return false;
    }

    /**
     * Detrás de nginx, getRemoteAddr() devuelve la IP del proxy para todos los
     * clientes, lo que haría que todos compartan el mismo bucket. Se usa el
     * primer valor de X-Forwarded-For cuando está presente.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class TokenBucket {
        private final int maxTokens;
        private final long refillDuration;
        private int tokens;
        private long lastRefillTimestamp;

        public TokenBucket(int maxTokens, long refillDuration) {
            this.maxTokens = maxTokens;
            this.refillDuration = refillDuration;
            this.tokens = maxTokens;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            if (now - lastRefillTimestamp > refillDuration) {
                tokens = maxTokens;
                lastRefillTimestamp = now;
            }
        }
    }
}
