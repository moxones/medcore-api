package com.medical.medcore.schedule.domain;

public enum ExceptionReason {
    VACATION("Vacaciones"),
    SICK_LEAVE("Salud"),
    PERSONAL("Personal"),
    TRAINING("Capacitación"),
    UNPLANNED_ABSENCE("No se presentó"),
    EXTRA_HOURS("Horas extra"),
    OTHER("Otro");

    private final String label;

    ExceptionReason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
