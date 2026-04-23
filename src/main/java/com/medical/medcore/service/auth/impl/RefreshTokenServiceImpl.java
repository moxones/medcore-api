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

        return repository.findAll().stream()
                .filter(rt -> !rt.getIsRevoked())
                .filter(rt -> passwordEncoder.matches(rawToken, rt.getToken()))
                .filter(rt -> rt.getExpiresAt().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }

    @Override
    public void revoke(RefreshToken token) {
        token.revoke();
        repository.save(token);
    }
}