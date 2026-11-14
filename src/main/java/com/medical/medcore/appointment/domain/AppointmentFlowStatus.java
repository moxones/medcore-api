package com.medical.medcore.appointment.domain;

import java.util.Map;
import java.util.Set;

public enum AppointmentFlowStatus {
    SCHEDULED,
    WAITING,
    CALLED,
    IN_PROCESS,
    PENDING_PAYMENT,
    COMPLETED;

    private static final Map<AppointmentFlowStatus, Set<AppointmentFlowStatus>> ALLOWED = Map.of(
            SCHEDULED, Set.of(WAITING),
            WAITING, Set.of(CALLED, IN_PROCESS),
            CALLED, Set.of(IN_PROCESS, WAITING),
            IN_PROCESS, Set.of(PENDING_PAYMENT, COMPLETED, CALLED),
            PENDING_PAYMENT, Set.of(COMPLETED, IN_PROCESS),
            COMPLETED, Set.of()
    );

    public boolean canTransitionTo(AppointmentFlowStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isBeforeConsultation() {
        return this == SCHEDULED || this == WAITING || this == CALLED;
    }

    public boolean isAttended() {
        return this == PENDING_PAYMENT || this == COMPLETED;
    }

    public static AppointmentFlowStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return SCHEDULED;
        }
        return AppointmentFlowStatus.valueOf(raw.trim().toUpperCase());
    }
}
