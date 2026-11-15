package com.medical.medcore.platform.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ClinicModule {
    TRIAGE("Triaje"),
    CONSULTATION("Consulta e historia clínica"),
    BILLING("Cobros"),
    PATIENT_PORTAL("Portal del paciente"),
    REPORTS("Reportes");

    private final String label;

    ClinicModule(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public Set<ClinicModule> requires() {
        return this == TRIAGE ? EnumSet.of(CONSULTATION) : EnumSet.noneOf(ClinicModule.class);
    }

    public static Set<ClinicModule> all() {
        return EnumSet.allOf(ClinicModule.class);
    }
}
