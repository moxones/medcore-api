package com.medical.medcore.platform.domain;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public final class SubscriptionAccessPolicy {

    private static final Set<String> SERVING_STATUSES = Set.of(Subscription.STATUS_ACTIVE, "TRIAL");

    private final int graceDays;

    public SubscriptionAccessPolicy(int graceDays) {
        this.graceDays = graceDays;
    }

    public boolean blocks(Optional<Subscription> latest, LocalDate today) {
        return latest.map(subscription -> blocks(subscription, today)).orElse(false);
    }

    private boolean blocks(Subscription subscription, LocalDate today) {
        if (!SERVING_STATUSES.contains(subscription.getStatus())) {
            return true;
        }
        LocalDate endDate = subscription.getEndDate();
        return endDate != null && endDate.plusDays(graceDays).isBefore(today);
    }
}
