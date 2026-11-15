package com.medical.medcore.patient.domain;

import com.medical.medcore.identity.domain.User;

public enum PortalAccountStatus {
    NONE,
    PENDING_ACTIVATION,
    ACTIVE,
    INACTIVE;

    public static PortalAccountStatus of(User account) {
        if (account == null) {
            return NONE;
        }
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            return INACTIVE;
        }
        return account.isPendingActivation() ? PENDING_ACTIVATION : ACTIVE;
    }
}
