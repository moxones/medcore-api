package com.medical.medcore.platform.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionAccessPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    private final SubscriptionAccessPolicy policy = new SubscriptionAccessPolicy(7);

    @Test
    void tenantWithoutSubscriptionIsNotBlocked() {
        assertThat(policy.blocks(Optional.empty(), TODAY)).isFalse();
    }

    @Test
    void activeOpenEndedSubscriptionServes() {
        assertThat(policy.blocks(Optional.of(subscription("ACTIVE", null)), TODAY)).isFalse();
    }

    @Test
    void endedSubscriptionServesDuringGracePeriod() {
        assertThat(policy.blocks(Optional.of(subscription("ACTIVE", TODAY.minusDays(7))), TODAY)).isFalse();
        assertThat(policy.blocks(Optional.of(subscription("TRIAL", TODAY.minusDays(8))), TODAY)).isTrue();
    }

    @Test
    void terminalStatusesBlock() {
        for (String status : new String[] {"CANCELLED", "SUSPENDED", "EXPIRED", "INACTIVE"}) {
            assertThat(policy.blocks(Optional.of(subscription(status, TODAY.plusDays(30))), TODAY)).isTrue();
        }
    }

    private Subscription subscription(String status, LocalDate endDate) {
        return Subscription.builder().status(status).startDate(TODAY.minusMonths(1)).endDate(endDate).build();
    }
}
