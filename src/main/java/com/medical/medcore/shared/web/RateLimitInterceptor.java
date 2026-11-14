package com.medical.medcore.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.medcore.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private static final String CODE_RATE_LIMITED = "RATE_LIMITED";

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowMs;

    public RateLimitInterceptor(ObjectMapper objectMapper,
                                @Value("${app.rate-limit.max-requests:300}") int maxRequests,
                                @Value("${app.rate-limit.window-ms:60000}") long windowMs) {
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (buckets.size() > MAX_TRACKED_CLIENTS) {
            buckets.clear();
        }
        TokenBucket bucket = buckets.computeIfAbsent(ClientIpResolver.resolve(request),
                key -> new TokenBucket(maxRequests, windowMs));
        if (bucket.tryConsume()) {
            return true;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("Demasiadas solicitudes, intente nuevamente en un momento", CODE_RATE_LIMITED));
        return false;
    }

    private static final class TokenBucket {

        private final int capacity;
        private final long windowMs;
        private int tokens;
        private long windowStart;

        private TokenBucket(int capacity, long windowMs) {
            this.capacity = capacity;
            this.windowMs = windowMs;
            this.tokens = capacity;
            this.windowStart = System.currentTimeMillis();
        }

        private synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMs) {
                tokens = capacity;
                windowStart = now;
            }
            if (tokens <= 0) {
                return false;
            }
            tokens--;
            return true;
        }
    }
}
