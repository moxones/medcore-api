package com.medical.medcore.patient.domain;

import com.medical.medcore.identity.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortalAccountStatusTest {

    @Test
    void describesThePortalAccountOfAPatient() {
        assertThat(PortalAccountStatus.of(null)).isEqualTo(PortalAccountStatus.NONE);
        assertThat(PortalAccountStatus.of(User.builder().pendingActivation(true).build()))
                .isEqualTo(PortalAccountStatus.PENDING_ACTIVATION);
        assertThat(PortalAccountStatus.of(User.builder().build())).isEqualTo(PortalAccountStatus.ACTIVE);
        assertThat(PortalAccountStatus.of(User.builder().isActive(false).pendingActivation(true).build()))
                .isEqualTo(PortalAccountStatus.INACTIVE);
    }
}
