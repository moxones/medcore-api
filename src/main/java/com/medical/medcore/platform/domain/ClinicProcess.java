package com.medical.medcore.platform.domain;

import java.util.Set;

public enum ClinicProcess {
    TRIAGE(ClinicModule.TRIAGE, true),
    PAYMENT(ClinicModule.BILLING, true),
    CALLED(null, true),
    ABSENCE_APPROVAL(null, false);

    private final ClinicModule module;
    private final boolean activeByDefault;

    ClinicProcess(ClinicModule module, boolean activeByDefault) {
        this.module = module;
        this.activeByDefault = activeByDefault;
    }

    public boolean activeByDefault() {
        return activeByDefault;
    }

    public boolean availableWith(Set<ClinicModule> modules) {
        return module == null || modules.contains(module);
    }
}
