package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.RefreshToken;
import com.medical.medcore.identity.domain.Role;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.identity.dto.ChangePasswordRequest;
import com.medical.medcore.identity.dto.LoginRequest;
import com.medical.medcore.identity.dto.UserMeResponse;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.exception.UnauthorizedException;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    private static final String CODE_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    private static final String CODE_ACCOUNT_INACTIVE = "ACCOUNT_INACTIVE";
    private static final String INVALID_CREDENTIALS = "Credenciales inválidas";

    private final UserRepository userRepository;
    private final UserAccounts userAccounts;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.auth.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.auth.lock-minutes:15}")
    private int lockMinutes;

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse login(LoginRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String email = PersonRegistry.normalizeEmail(request.email());
        User user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new UnauthorizedException(CODE_INVALID_CREDENTIALS, INVALID_CREDENTIALS));

        LocalDateTime now = LocalDateTime.now();
        if (user.isLockedAt(now)) {
            throw new UnauthorizedException(CODE_ACCOUNT_LOCKED,
                    "Cuenta bloqueada temporalmente por intentos fallidos. Intente nuevamente más tarde");
        }
        if (!userAccounts.matches(request.password(), user)) {
            user.registerFailedLogin(maxFailedAttempts, now.plusMinutes(lockMinutes));
            userRepository.save(user);
            throw new UnauthorizedException(CODE_INVALID_CREDENTIALS, INVALID_CREDENTIALS);
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException(CODE_ACCOUNT_INACTIVE, "Usuario inactivo. Contacte al administrador");
        }
        user.registerSuccessfulLogin(now);
        userRepository.save(user);
        return tokenIssuer.issue(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokenService.validate(rawRefreshToken);
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(token.getTenantId())) {
            throw new UnauthorizedException("El token no pertenece a esta organización");
        }
        User user = userRepository.findByIdAndTenantId(token.getUserId(), token.getTenantId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException(CODE_ACCOUNT_INACTIVE, "Usuario inactivo");
        }
        refreshTokenService.revokeWithGrace(token);
        return tokenIssuer.issue(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeIfPresent(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserMeResponse me() {
        User user = currentUser();
        return new UserMeResponse(
                user.getId(),
                user.getPerson().getId(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                user.getPerson().getFirstName(),
                user.getPerson().getLastName(),
                user.getTenantId(),
                Boolean.TRUE.equals(user.getPerson().getProfileCompleted()),
                Boolean.TRUE.equals(user.getMustChangePassword()),
                tokenIssuer.branchIdsOf(user));
    }

    @Transactional
    public AuthResponse changeOwnPassword(ChangePasswordRequest request) {
        User user = currentUser();
        if (!userAccounts.matches(request.currentPassword(), user)) {
            throw new BadRequestException("INVALID_CURRENT_PASSWORD", "La contraseña actual es incorrecta");
        }
        if (userAccounts.matches(request.newPassword(), user)) {
            throw new BadRequestException("La nueva contraseña debe ser distinta de la actual");
        }
        user.changePassword(userAccounts.encode(request.newPassword()), LocalDateTime.now(), false);
        userRepository.save(user);
        refreshTokenService.revokeAll(user.getId(), user.getTenantId());
        return tokenIssuer.issue(user);
    }

    private User currentUser() {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ForbiddenException(CODE_ACCOUNT_INACTIVE, "Usuario inactivo");
        }
        return user;
    }
}
