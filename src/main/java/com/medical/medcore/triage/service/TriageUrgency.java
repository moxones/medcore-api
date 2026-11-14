package com.medical.medcore.triage.service;

import java.util.Set;

public enum TriageUrgency {
    NORMAL,
    URGENT,
    CRITICAL;

    private static final String MANCHESTER = "MANCHESTER";
    private static final Set<String> MANCHESTER_CRITICAL = Set.of("RED", "ORANGE", "ROJO", "NARANJA", "1", "2");
    private static final Set<String> MANCHESTER_URGENT = Set.of("YELLOW", "AMARILLO", "3");
    private static final int ESI_CRITICAL_MAX = 2;
    private static final int ESI_URGENT = 3;

    public static TriageUrgency from(String prioritySystem, String priorityLevel) {
        if (priorityLevel == null || priorityLevel.isBlank()) {
            return NORMAL;
        }
        String level = priorityLevel.trim().toUpperCase();
        if (MANCHESTER.equalsIgnoreCase(prioritySystem)) {
            return fromManchester(level);
        }
        return fromEsi(level);
    }

    private static TriageUrgency fromManchester(String level) {
        if (MANCHESTER_CRITICAL.contains(level)) {
            return CRITICAL;
        }
        return MANCHESTER_URGENT.contains(level) ? URGENT : NORMAL;
    }

    private static TriageUrgency fromEsi(String level) {
        try {
            int value = Integer.parseInt(level);
            if (value <= ESI_CRITICAL_MAX) {
                return CRITICAL;
            }
            return value == ESI_URGENT ? URGENT : NORMAL;
        } catch (NumberFormatException notNumeric) {
            return NORMAL;
        }
    }
}
