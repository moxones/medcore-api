package com.medical.medcore.service.auth.impl;

import com.medical.medcore.entity.RefreshToken;
import com.medical.medcore.repository.RefreshTokenRepository;
import com.medical.medcore.service.auth.RefreshTokenService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    /**
     * Ventana de gracia tras una rotación: el token viejo sigue siendo válido
     * estos segundos para que dos refresh concurrentes del mismo cliente no
     * cierren la sesión (el segundo llegaría con el token recién rotado).
     */
    private static final long ROTATION_GRACE_SECONDS = 30;

    private final RefreshTokenRepository repository;

    public RefreshTokenServiceImpl(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public String create(Long userId, Long tenantId) {

        String raw = UUID.randomUUID().toString();

        RefreshToken entity = new RefreshToken();
        entity.userId = userId;
        entity.tenantId = tenantId;
        entity.token = sha256(raw);
        entity.expiresAt = LocalDateTime.now().plusDays(7);
        entity.isRevoked = false;

        repository.save(entity);

        return raw;
    }

    @Override
    public RefreshToken validate(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new com.medical.medcore.config.exception.BadRequestException("Token no encontrado o inválido");
        }

        RefreshToken token = repository.findByToken(sha256(rawToken))
                .orElseThrow(() -> new com.medical.medcore.config.exception.BadRequestException("Token no encontrado o inválido"));

        if (token.getIsRevoked()) {
            throw new com.medical.medcore.config.exception.BadRequestException("El refresh token ha sido revocado");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.revoke();
            repository.save(token);
            throw new com.medical.medcore.config.exception.BadRequestException("El refresh token ha expirado");
        }

        return token;
    }

    @Override
    public void revoke(RefreshToken token) {
        token.revoke();
        repository.save(token);
    }

    @Override
    public void revokeWithGrace(RefreshToken token) {
        LocalDateTime graceLimit = LocalDateTime.now().plusSeconds(ROTATION_GRACE_SECONDS);
        if (token.getExpiresAt().isAfter(graceLimit)) {
            token.setExpiresAt(graceLimit);
            repository.save(token);
        }
    }

    @Override
    public void revokeAllByUserIdAndTenantId(Long userId, Long tenantId) {
        java.util.List<RefreshToken> activeTokens = repository.findByUserIdAndTenantIdAndIsRevokedFalse(userId, tenantId);
        activeTokens.forEach(RefreshToken::revoke);
        repository.saveAll(activeTokens);
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "America/Lima")
    @Transactional
    public void purgeExpiredAndRevoked() {
        int deleted = repository.deleteExpiredOrRevoked(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Refresh tokens purgados: {}", deleted);
        }
    }

    /**
     * Hash determinístico (a diferencia de BCrypt) para poder buscar el token
     * por índice en lugar de recorrer toda la tabla. El token es un UUID
     * aleatorio, por lo que no necesita salt. Base64 url-safe: 43 caracteres.
     */
    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
