package com.medical.medcore.service.auth.impl;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.LoginRequest;
import com.medical.medcore.dto.response.AuthResponse;
import com.medical.medcore.entity.RefreshToken;
import com.medical.medcore.entity.Tenant;
import com.medical.medcore.entity.User;
import com.medical.medcore.entity.enums.TenantStatus;
import com.medical.medcore.repository.TenantRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.security.jwt.JwtProvider;
import com.medical.medcore.service.auth.AuthService;
import com.medical.medcore.service.auth.RefreshTokenService;
import com.medical.medcore.util.TenantContext;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TenantRepository tenantRepository,
                           JwtProvider jwtProvider,
                           RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository;
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new BadRequestException("Tenant no resuelto");
        }

        Tenant tenant = tenantRepository
                .findByIdAndStatus(tenantId, TenantStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Tenant no encontrado o inactivo"));

        User user = userRepository
                .findByEmailAndTenantId(request.getEmail(), tenant.getId())
                .orElseThrow(() -> new BadRequestException("Credenciales inválidas"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Credenciales inválidas");
        }

        List<String> roles = extractRoles(user);

        String accessToken = jwtProvider.generateToken(
                user.getId(),
                tenant.getId(),
                roles
        );

        String refreshToken = refreshTokenService.create(
                user.getId(),
                tenant.getId()
        );

        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refresh(String refreshTokenRaw) {

        RefreshToken token = refreshTokenService.validate(refreshTokenRaw);

        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(token.getTenantId())) {
            throw new BadRequestException("Token no pertenece a este tenant");
        }

        Tenant tenant = tenantRepository
                .findByIdAndStatus(token.getTenantId(), TenantStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Tenant inactivo"));

        User user = userRepository
                .findByIdAndTenantId(token.getUserId(), token.getTenantId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Usuario inactivo");
        }

        List<String> roles = extractRoles(user);

        String newAccess = jwtProvider.generateToken(
                user.getId(),
                tenant.getId(),
                roles
        );

        refreshTokenService.revoke(token);

        String newRefresh = refreshTokenService.create(
                user.getId(),
                tenant.getId()
        );

        return new AuthResponse(newAccess, newRefresh);
    }

    @Override
    public void logout(String refreshTokenRaw) {

        try {
            RefreshToken token = refreshTokenService.validate(refreshTokenRaw);
            refreshTokenService.revoke(token);
        } catch (Exception ignored) {
            // logout debe ser idempotente
        }
    }

    private List<String> extractRoles(User user) {
        return user.getRoles()
                .stream()
                .map(r -> r.getCode())
                .toList();
    }
}