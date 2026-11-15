package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.RefreshToken;
import com.medical.medcore.identity.repository.RefreshTokenRepository;
import com.medical.medcore.shared.exception.UnauthorizedException;
import com.medical.medcore.shared.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long ROTATION_GRACE_SECONDS = 30;
    private static final String CODE_INVALID_REFRESH = "INVALID_REFRESH_TOKEN";

    private final RefreshTokenRepository repository;

    @Value("${security.jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public String create(Long userId, Long tenantId) {
        String raw = TokenHasher.newOpaqueToken();
        repository.save(RefreshToken.builder()
                .userId(userId)
                .tenantId(tenantId)
                .token(TokenHasher.sha256(raw))
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .build());
        return raw;
    }

    @Transactional
    public RefreshToken validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalid("Token no encontrado o inválido");
        }
        RefreshToken token = repository.findByToken(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> invalid("Token no encontrado o inválido"));
        if (Boolean.TRUE.equals(token.getIsRevoked())) {
            throw invalid("El refresh token ha sido revocado");
        }
        if (token.isExpiredAt(LocalDateTime.now())) {
            throw invalid("El refresh token ha expirado");
        }
        return token;
    }

    @Transactional
    public void revokeIfPresent(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByToken(TokenHasher.sha256(rawToken)).ifPresent(token -> {
            token.revoke();
            repository.save(token);
        });
    }

    @Transactional
    public void revokeWithGrace(RefreshToken token) {
        LocalDateTime graceLimit = LocalDateTime.now().plusSeconds(ROTATION_GRACE_SECONDS);
        if (token.getExpiresAt().isAfter(graceLimit)) {
            token.setExpiresAt(graceLimit);
            repository.save(token);
        }
    }

    @Transactional
    public void revokeAll(Long userId, Long tenantId) {
        List<RefreshToken> active = repository.findByUserIdAndTenantIdAndIsRevokedFalse(userId, tenantId);
        active.forEach(RefreshToken::revoke);
        repository.saveAll(active);
    }

    @Transactional
    public void purgeExpiredAndRevoked() {
        int deleted = repository.deleteExpiredOrRevoked(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Refresh tokens purgados: {}", deleted);
        }
    }

    private UnauthorizedException invalid(String message) {
        return new UnauthorizedException(CODE_INVALID_REFRESH, message);
    }
}
