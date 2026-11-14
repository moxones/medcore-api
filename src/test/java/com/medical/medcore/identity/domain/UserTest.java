package com.medical.medcore.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 1, 8, 0);
    private static final int MAX_ATTEMPTS = 3;

    @Test
    void locksAfterReachingTheMaximumFailedAttempts() {
        User user = User.builder().build();

        user.registerFailedLogin(MAX_ATTEMPTS, NOW.plusMinutes(15));
        user.registerFailedLogin(MAX_ATTEMPTS, NOW.plusMinutes(15));
        assertThat(user.isLockedAt(NOW)).isFalse();

        user.registerFailedLogin(MAX_ATTEMPTS, NOW.plusMinutes(15));
        assertThat(user.isLockedAt(NOW)).isTrue();
        assertThat(user.isLockedAt(NOW.plusMinutes(16))).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void aSuccessfulLoginClearsTheFailureHistory() {
        User user = User.builder().build();
        user.registerFailedLogin(MAX_ATTEMPTS, NOW.plusMinutes(15));

        user.registerSuccessfulLogin(NOW);

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isEqualTo(NOW);
    }

    @Test
    void changingThePasswordUnlocksAndTracksTheForcedChangeFlag() {
        User user = User.builder().password("old").build();
        user.setLockedUntil(NOW.plusHours(1));

        user.changePassword("new-hash", NOW, true);

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(user.getPasswordChangedAt()).isEqualTo(NOW);
        assertThat(user.getMustChangePassword()).isTrue();
        assertThat(user.isLockedAt(NOW)).isFalse();
    }

    @Test
    void checksRolesByCode() {
        User user = User.builder().roles(Set.of(new Role(3L, "Doctor", "DOCTOR"))).build();

        assertThat(user.hasRole("DOCTOR")).isTrue();
        assertThat(user.hasRole("PATIENT")).isFalse();
    }
}
