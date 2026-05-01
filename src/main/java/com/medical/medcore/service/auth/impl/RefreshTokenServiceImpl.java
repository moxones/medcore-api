package com.medical.medcore.service.auth.impl;

import com.medical.medcore.entity.RefreshToken;
import com.medical.medcore.repository.RefreshTokenRepository;
import com.medical.medcore.service.auth.RefreshTokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final PasswordEncoder passwordEncoder;

    public RefreshTokenServiceImpl(RefreshTokenRepository repository,
                                   PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String create(Long userId, Long tenantId) {

        String raw = UUID.randomUUID().toString();
        String hash = passwordEncoder.encode(raw);

        RefreshToken entity = new RefreshToken();
        entity.userId = userId;
        entity.tenantId = tenantId;
        entity.token = hash;
        entity.expiresAt = LocalDateTime.now().plusDays(7);
        entity.isRevoked = false;

        repository.save(entity);

        return raw;
    }

    @Override
    public RefreshToken validate(String rawToken) {

        RefreshToken token = repository.findAll().stream()
                .filter(rt -> passwordEncoder.matches(rawToken, rt.getToken()))
                .findFirst()
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
    public void revokeAllByUserIdAndTenantId(Long userId, Long tenantId) {
        java.util.List<RefreshToken> activeTokens = repository.findByUserIdAndTenantIdAndIsRevokedFalse(userId, tenantId);
        activeTokens.forEach(RefreshToken::revoke);
        repository.saveAll(activeTokens);
    }
}