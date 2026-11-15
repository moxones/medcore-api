package com.medical.medcore.identity.service;

import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CredentialMaintenance {

    private final RefreshTokenService refreshTokenService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final EmailVerificationService emailVerificationService;

    @Scheduled(cron = "0 0 4 * * *", zone = "America/Lima")
    public void purgeRefreshTokens() {
        TenantContext.runAsSystem(refreshTokenService::purgeExpiredAndRevoked);
    }

    @Scheduled(cron = "0 30 4 * * *", zone = "America/Lima")
    public void purgeResetTokens() {
        TenantContext.runAsSystem(passwordRecoveryService::purgeExpired);
        TenantContext.runAsSystem(emailVerificationService::purgeExpired);
    }
}
