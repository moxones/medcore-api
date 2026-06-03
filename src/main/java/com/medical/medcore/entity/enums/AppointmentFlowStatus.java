package com.medical.medcore.entity.enums;

import java.util.Map;
import java.util.Set;

/**
 * Estados del recorrido operativo de una cita en la "Sala de espera".
 *
 * Orden lógico del flujo:
 *   SCHEDULED -> WAITING -> CALLED -> IN_PROCESS -> PENDING_PAYMENT -> COMPLETED
 *
 * Compatibilidad: WAITING, IN_PROCESS y COMPLETED ya existían; CALLED y
 * PENDING_PAYMENT son nuevos. Un flowStatus null en datos legados se interpreta
 * como SCHEDULED.
 */
public enum AppointmentFlowStatus {
    SCHEDULED,
    WAITING,
    CALLED,
    IN_PROCESS,
    PENDING_PAYMENT,
    COMPLETED;

    /**
     * Transiciones permitidas desde cada estado. Incluye:
     *  - avance normal de un paso,
     *  - retroceso de un paso (recepción se equivoca), nunca de vuelta a SCHEDULED,
     *  - atajos ágiles: WAITING -> IN_PROCESS e IN_PROCESS -> COMPLETED.
     * COMPLETED es terminal.
     */
    private static final Map<AppointmentFlowStatus, Set<AppointmentFlowStatus>> ALLOWED = Map.of(
            SCHEDULED,       Set.of(WAITING),
            WAITING,         Set.of(CALLED, IN_PROCESS),
            CALLED,          Set.of(IN_PROCESS, WAITING),
            IN_PROCESS,      Set.of(PENDING_PAYMENT, COMPLETED, CALLED),
            PENDING_PAYMENT, Set.of(COMPLETED, IN_PROCESS),
            COMPLETED,       Set.of()
    );

    public boolean canTransitionTo(AppointmentFlowStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    /**
     * Resuelve un valor crudo (incluido null) a un estado del enum.
     * @throws IllegalArgumentException si el string no corresponde a un estado válido.
     */
    public static AppointmentFlowStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return SCHEDULED;
        }
        return AppointmentFlowStatus.valueOf(raw.trim().toUpperCase());
    }
}
