package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.PasswordResetToken;
import com.medical.medcore.identity.domain.TokenPurpose;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.ForgotPasswordResponse;
import com.medical.medcore.identity.dto.ResetPasswordRequest;
import com.medical.medcore.identity.repository.PasswordResetTokenRepository;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.platform.service.TenantMailBranding;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.notification.MailTemplates;
import com.medical.medcore.shared.notification.NotificationSender;
import com.medical.medcore.shared.security.OneTimeCode;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Recuperación de contraseña con un código de 6 dígitos enviado al correo de la cuenta. La persona se identifica con
 * su correo o su documento; el código vence en minutos, admite pocos intentos y no se puede pedir otro hasta que
 * pase la espera de reenvío. Las respuestas no revelan si la cuenta existe.
 */
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final String CODE_INVALID_RESET_CODE = "INVALID_RESET_CODE";
    private static final String INVALID_CODE = "El código es incorrecto o ya venció. Revisa tu correo o pide uno nuevo";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserAccounts userAccounts;
    private final RefreshTokenService refreshTokenService;
    private final NotificationSender notificationSender;
    private final TenantMailBranding mailBranding;
    private final AccountLookup accountLookup;

    @Value("${app.auth.reset-code-minutes:10}")
    private int resetCodeMinutes;

    @Value("${app.auth.code-max-attempts:5}")
    private int maxAttempts;

    @Value("${app.auth.code-resend-seconds:60}")
    private int resendSeconds;

    @Value("${app.auth.expose-reset-token:false}")
    private boolean exposeResetCode;

    @Transactional
    public ForgotPasswordResponse requestReset(String identifier, String requesterIp) {
        Long tenantId = TenantContext.requireTenantId();
        String code = accountLookup.find(tenantId, identifier)
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .map(user -> issueCode(user, requesterIp))
                .orElse(null);
        return new ForgotPasswordResponse(exposeResetCode ? code : null, resetCodeMinutes, resendSeconds);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void resetPassword(ResetPasswordRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        LocalDateTime now = LocalDateTime.now();
        User user = accountLookup.find(tenantId, request.identifier())
                .filter(candidate -> Boolean.TRUE.equals(candidate.getIsActive()))
                .orElseThrow(PasswordRecoveryService::invalidCode);
        PasswordResetToken token = latestCode(user)
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(PasswordRecoveryService::invalidCode);
        if (!matches(token, user, request.code())) {
            token.registerFailedAttempt(maxAttempts, now);
            tokenRepository.save(token);
            throw invalidCode();
        }
        user.changePassword(userAccounts.encode(request.newPassword()), now, false);
        userRepository.save(user);
        token.markUsed(now);
        tokenRepository.save(token);
        refreshTokenService.revokeAll(user.getId(), tenantId);
    }

    @Transactional
    public void purgeExpired() {
        tokenRepository.deleteExpired(LocalDateTime.now().minusDays(1));
    }

    private String issueCode(User user, String requesterIp) {
        LocalDateTime now = LocalDateTime.now();
        boolean recentlySent = latestCode(user)
                .filter(token -> token.isUsableAt(now))
                .filter(token -> token.getCreatedAt().isAfter(now.minusSeconds(resendSeconds)))
                .isPresent();
        if (recentlySent) {
            return null;
        }
        tokenRepository.invalidatePending(user.getId(), TokenPurpose.PASSWORD_RESET, now);
        String code = OneTimeCode.generate();
        tokenRepository.save(PasswordResetToken.builder()
                .tenantId(user.getTenantId())
                .userId(user.getId())
                .tokenHash(OneTimeCode.hash(owner(user), code))
                .purpose(TokenPurpose.PASSWORD_RESET)
                .expiresAt(now.plusMinutes(resetCodeMinutes))
                .requestedIp(requesterIp)
                .createdAt(now)
                .build());
        notificationSender.send(MailTemplates.code(mailBranding.forTenant(user.getTenantId()), user.getTenantId(),
                user.getEmail(), MailTemplates.CodePurpose.PASSWORD_RESET, code, resetCodeMinutes));
        return code;
    }

    private Optional<PasswordResetToken> latestCode(User user) {
        return tokenRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDescIdDesc(user.getId(),
                TokenPurpose.PASSWORD_RESET);
    }

    private static boolean matches(PasswordResetToken token, User user, String code) {
        byte[] expected = token.getTokenHash().getBytes(StandardCharsets.UTF_8);
        byte[] actual = OneTimeCode.hash(owner(user), code).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String owner(User user) {
        return "user:" + user.getId();
    }

    private static BadRequestException invalidCode() {
        return new BadRequestException(CODE_INVALID_RESET_CODE, INVALID_CODE);
    }
}
