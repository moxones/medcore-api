package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.PasswordResetToken;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.ForgotPasswordResponse;
import com.medical.medcore.identity.dto.ResetPasswordRequest;
import com.medical.medcore.identity.repository.PasswordResetTokenRepository;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.notification.Notification;
import com.medical.medcore.shared.notification.NotificationSender;
import com.medical.medcore.shared.security.TokenHasher;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final String CODE_INVALID_RESET_TOKEN = "INVALID_RESET_TOKEN";
    private static final String INVALID_LINK = "El enlace de recuperación es inválido o ha expirado";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserAccounts userAccounts;
    private final RefreshTokenService refreshTokenService;
    private final NotificationSender notificationSender;

    @Value("${app.auth.reset-token-minutes:30}")
    private int resetTokenMinutes;

    @Value("${app.auth.expose-reset-token:false}")
    private boolean exposeResetToken;

    @Transactional
    public ForgotPasswordResponse requestReset(String email, String requesterIp) {
        Long tenantId = TenantContext.requireTenantId();
        String normalizedEmail = PersonRegistry.normalizeEmail(email);
        return userRepository.findByEmailAndTenantId(normalizedEmail, tenantId)
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .map(user -> issueToken(user, requesterIp))
                .orElseGet(() -> new ForgotPasswordResponse(null, resetTokenMinutes));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = tokenRepository.findByTokenHash(TokenHasher.sha256(request.token()))
                .filter(candidate -> candidate.getTenantId().equals(tenantId))
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(() -> new BadRequestException(CODE_INVALID_RESET_TOKEN, INVALID_LINK));

        User user = userRepository.findByIdAndTenantId(token.getUserId(), tenantId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getIsActive()))
                .orElseThrow(() -> new BadRequestException(CODE_INVALID_RESET_TOKEN, INVALID_LINK));

        user.changePassword(userAccounts.encode(request.newPassword()), now, false);
        userRepository.save(user);
        token.markUsed(now);
        tokenRepository.save(token);
        refreshTokenService.revokeAll(user.getId(), tenantId);
    }

    @Scheduled(cron = "0 30 4 * * *", zone = "America/Lima")
    @Transactional
    public void purgeExpired() {
        tokenRepository.deleteExpired(LocalDateTime.now().minusDays(1));
    }

    private ForgotPasswordResponse issueToken(User user, String requesterIp) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.invalidatePending(user.getId(), now);
        String rawToken = TokenHasher.newOpaqueToken();
        tokenRepository.save(PasswordResetToken.builder()
                .tenantId(user.getTenantId())
                .userId(user.getId())
                .tokenHash(TokenHasher.sha256(rawToken))
                .expiresAt(now.plusMinutes(resetTokenMinutes))
                .requestedIp(requesterIp)
                .createdAt(now)
                .build());
        notificationSender.send(new Notification(
                user.getTenantId(),
                user.getEmail(),
                "Recuperación de contraseña",
                "Usa este código para restablecer tu contraseña: " + rawToken));
        return new ForgotPasswordResponse(exposeResetToken ? rawToken : null, resetTokenMinutes);
    }
}
