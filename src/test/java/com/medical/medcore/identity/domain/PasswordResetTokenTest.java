package com.medical.medcore.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 10, 0);
    private static final int MAX_ATTEMPTS = 3;

    @Test
    void aCodeStopsWorkingAfterTheMaximumFailedAttempts() {
        PasswordResetToken token = PasswordResetToken.builder().expiresAt(NOW.plusMinutes(10)).build();

        token.registerFailedAttempt(MAX_ATTEMPTS, NOW);
        token.registerFailedAttempt(MAX_ATTEMPTS, NOW);
        assertThat(token.isUsableAt(NOW)).isTrue();
        assertThat(token.hasAttemptsLeft(MAX_ATTEMPTS)).isTrue();

        token.registerFailedAttempt(MAX_ATTEMPTS, NOW);
        assertThat(token.isUsableAt(NOW)).isFalse();
        assertThat(token.hasAttemptsLeft(MAX_ATTEMPTS)).isFalse();
    }

    @Test
    void anExpiredCodeIsNotUsable() {
        PasswordResetToken token = PasswordResetToken.builder().expiresAt(NOW.minusSeconds(1)).build();

        assertThat(token.isUsableAt(NOW)).isFalse();
    }
}
