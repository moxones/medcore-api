package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.EmailVerification;
import com.medical.medcore.identity.dto.EmailVerificationResponse;
import com.medical.medcore.identity.repository.EmailVerificationRepository;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.platform.service.TenantMailBranding;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Confirma que quien se registra en la web es dueño del correo, con un código de 6 dígitos que se pide antes de crear
 * la cuenta. Si el correo no se puede enviar (no hay canal configurado o el servidor lo rechazó) la verificación se
 * omite: la persona no queda bloqueada por una falla del servicio de correo.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_EMAIL_TAKEN = "EMAIL_ALREADY_REGISTERED";
    private static final String CODE_REQUIRED = "EMAIL_CODE_REQUIRED";
    private static final String CODE_INVALID = "INVALID_EMAIL_CODE";

    private final EmailVerificationRepository repository;
    private final EmailVerificationAttempts attempts;
    private final UserRepository userRepository;
    private final UserAccounts userAccounts;
    private final NotificationSender notificationSender;
    private final TenantMailBranding mailBranding;

    @Value("${app.auth.email-code-minutes:10}")
    private int codeMinutes;

    @Value("${app.auth.code-max-attempts:5}")
    private int maxAttempts;

    @Value("${app.auth.code-resend-seconds:60}")
    private int resendSeconds;

    @Value("${app.auth.expose-reset-token:false}")
    private boolean exposeCode;

    @Transactional
    public EmailVerificationResponse send(String rawEmail, String requesterIp) {
        Long tenantId = TenantContext.requireTenantId();
        String email = userAccounts.requireEmail(rawEmail);
        assertNotRegistered(email, tenantId);
        LocalDateTime now = LocalDateTime.now();
        Optional<EmailVerification> recent = latest(tenantId, email)
                .filter(verification -> verification.isUsableAt(now, maxAttempts))
                .filter(verification -> verification.getCreatedAt().isAfter(now.minusSeconds(resendSeconds)));
        if (recent.isPresent()) {
            long waited = Duration.between(recent.get().getCreatedAt(), now).toSeconds();
            return new EmailVerificationResponse(recent.get().wasDelivered(), codeMinutes,
                    (int) Math.max(1, resendSeconds - waited), null);
        }
        repository.expirePending(tenantId, email, now);
        String code = OneTimeCode.generate();
        EmailVerification verification = repository.save(EmailVerification.builder()
                .tenantId(tenantId)
                .email(email)
                .codeHash(OneTimeCode.hash(owner(email), code))
                .expiresAt(now.plusMinutes(codeMinutes))
                .requestedIp(requesterIp)
                .createdAt(now)
                .build());
        boolean delivered = notificationSender.send(MailTemplates.code(mailBranding.forTenant(tenantId), tenantId,
                email, MailTemplates.CodePurpose.EMAIL_VERIFICATION, code, codeMinutes));
        verification.setDelivered(delivered);
        repository.save(verification);
        return new EmailVerificationResponse(delivered, codeMinutes, resendSeconds, exposeCode ? code : null);
    }

    /**
     * Valida el código como parte del registro (misma transacción: si el registro falla, el código sigue sirviendo).
     * Sin código solo se acepta si el correo no se pudo enviar.
     */
    @Transactional
    public void consume(String email, String code) {
        Long tenantId = TenantContext.requireTenantId();
        LocalDateTime now = LocalDateTime.now();
        Optional<EmailVerification> usable = latest(tenantId, email)
                .filter(verification -> verification.isUsableAt(now, maxAttempts));
        if (OneTimeCode.normalize(code).isEmpty()) {
            consumeWithoutCode(usable, now);
            return;
        }
        EmailVerification verification = usable.orElseThrow(EmailVerificationService::invalidCode);
        if (!matches(verification, email, code)) {
            attempts.recordFailure(verification.getId());
            throw invalidCode();
        }
        verification.consume(now);
        repository.save(verification);
    }

    @Transactional
    public void purgeExpired() {
        repository.deleteExpired(LocalDateTime.now().minusDays(1));
    }

    private void consumeWithoutCode(Optional<EmailVerification> usable, LocalDateTime now) {
        if (!notificationSender.canDeliver()) {
            return;
        }
        EmailVerification undelivered = usable
                .filter(verification -> !verification.wasDelivered())
                .orElseThrow(() -> new BadRequestException(CODE_REQUIRED,
                        "Ingresa el código de verificación que enviamos a tu correo"));
        undelivered.consume(now);
        repository.save(undelivered);
    }

    private void assertNotRegistered(String email, Long tenantId) {
        boolean taken = userRepository.findByEmailAndTenantId(email, tenantId)
                .filter(user -> !user.isPendingActivation())
                .isPresent();
        if (taken) {
            throw new ConflictException(CODE_EMAIL_TAKEN,
                    "Este correo ya tiene una cuenta. Inicia sesión o recupera tu contraseña");
        }
    }

    private Optional<EmailVerification> latest(Long tenantId, String email) {
        return repository.findFirstByTenantIdAndEmailOrderByCreatedAtDescIdDesc(tenantId, email);
    }

    private static boolean matches(EmailVerification verification, String email, String code) {
        byte[] expected = verification.getCodeHash().getBytes(StandardCharsets.UTF_8);
        byte[] actual = OneTimeCode.hash(owner(email), code).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String owner(String email) {
        return "email:" + email;
    }

    private static BadRequestException invalidCode() {
        return new BadRequestException(CODE_INVALID, "El código es incorrecto o ya venció. Pide uno nuevo");
    }
}
