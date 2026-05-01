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

    private static final int MAX_REQUESTS = 60;
    private static final long REFILL_DURATION_MS = 60000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = request.getRemoteAddr();
        
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(MAX_REQUESTS, REFILL_DURATION_MS));
        
        if (bucket.tryConsume()) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        return false;
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
