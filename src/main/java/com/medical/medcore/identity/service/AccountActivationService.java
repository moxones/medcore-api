package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.PasswordResetToken;
import com.medical.medcore.identity.domain.TokenPurpose;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.ActivateAccountRequest;
import com.medical.medcore.identity.dto.ActivationInfoResponse;
import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.identity.repository.PasswordResetTokenRepository;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.platform.service.TenantMailBranding;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.notification.MailBranding;
import com.medical.medcore.shared.notification.MailTemplates;
import com.medical.medcore.shared.notification.NotificationSender;
import com.medical.medcore.shared.security.RoleConstants;
import com.medical.medcore.shared.security.TokenHasher;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountActivationService {

    private static final String CODE_INVALID_ACTIVATION = "INVALID_ACTIVATION_TOKEN";
    private static final String INVALID_LINK = "El enlace de activación es inválido o ya venció. Pide uno nuevo a la organización";
    private static final String ACTIVATION_PATH = "/activate-account?token=";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserAccounts userAccounts;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;
    private final NotificationSender notificationSender;
    private final TenantMailBranding mailBranding;

    @Value("${app.auth.activation-token-hours:72}")
    private int activationTokenHours;

    @Value("${app.auth.expose-reset-token:false}")
    private boolean exposeToken;

    /**
     * Emite el enlace de activación y lo envía por correo. Si el correo no sale, el ticket trae el token para que
     * quien invitó comparta el enlace por otro medio (en desarrollo siempre lo trae).
     */
    @Transactional
    public ActivationTicket issue(User user, String requesterIp) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.invalidatePending(user.getId(), TokenPurpose.ACTIVATION, now);
        String rawToken = TokenHasher.newOpaqueToken();
        LocalDateTime expiresAt = now.plusHours(activationTokenHours);
        tokenRepository.save(PasswordResetToken.builder()
                .tenantId(user.getTenantId())
                .userId(user.getId())
                .tokenHash(TokenHasher.sha256(rawToken))
                .purpose(TokenPurpose.ACTIVATION)
                .expiresAt(expiresAt)
                .requestedIp(requesterIp)
                .createdAt(now)
                .build());
        MailBranding brand = mailBranding.forTenant(user.getTenantId());
        String link = brand.link(ACTIVATION_PATH + URLEncoder.encode(rawToken, StandardCharsets.UTF_8));
        boolean delivered = notificationSender.send(MailTemplates.activation(brand, user.getTenantId(),
                user.getEmail(), user.getPerson().getFirstName(), link, activationTokenHours,
                user.hasRole(RoleConstants.PATIENT)));
        return new ActivationTicket(exposeToken || !delivered ? rawToken : null, expiresAt, delivered);
    }

    @Transactional(readOnly = true)
    public ActivationInfoResponse inspect(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = requireUsable(rawToken, now);
        User user = requirePendingUser(token);
        return new ActivationInfoResponse(user.getPerson().getFirstName(), maskEmail(user.getEmail()),
                token.getExpiresAt());
    }

    @Transactional
    public AuthResponse activate(ActivateAccountRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = requireUsable(request.token(), now);
        User user = requirePendingUser(token);
        user.changePassword(userAccounts.encode(request.password()), now, false);
        token.markUsed(now);
        tokenRepository.save(token);
        refreshTokenService.revokeAll(user.getId(), user.getTenantId());
        return authService.openSession(user, now);
    }

    public record ActivationTicket(String token, LocalDateTime expiresAt, boolean delivered) {
    }

    private PasswordResetToken requireUsable(String rawToken, LocalDateTime now) {
        Long tenantId = TenantContext.requireTenantId();
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException(CODE_INVALID_ACTIVATION, INVALID_LINK);
        }
        return tokenRepository.findByTokenHash(TokenHasher.sha256(rawToken.trim()))
                .filter(candidate -> candidate.getTenantId().equals(tenantId))
                .filter(candidate -> candidate.getPurpose() == TokenPurpose.ACTIVATION)
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(() -> new BadRequestException(CODE_INVALID_ACTIVATION, INVALID_LINK));
    }

    private User requirePendingUser(PasswordResetToken token) {
        return userRepository.findByIdAndTenantId(token.getUserId(), token.getTenantId())
                .filter(candidate -> Boolean.TRUE.equals(candidate.getIsActive()))
                .filter(User::isPendingActivation)
                .orElseThrow(() -> new BadRequestException(CODE_INVALID_ACTIVATION, INVALID_LINK));
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "*".repeat(Math.min(at - 1, 6)) + email.substring(at);
    }
}
