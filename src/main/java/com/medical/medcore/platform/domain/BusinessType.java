package com.medical.medcore.platform.domain;

import java.util.EnumSet;
import java.util.Set;

public enum BusinessType {
    MEDICAL("Clínica o centro médico", "médico", "médicos", "Dr.", "Dra.", ClinicModule.all()),
    PSYCHOLOGY("Consultorio de psicología", "psicólogo", "psicólogos", "Ps.", "Ps.", withoutTriage()),
    DENTAL("Consultorio odontológico", "odontólogo", "odontólogos", "Dr.", "Dra.", withoutTriage()),
    NUTRITION("Consultorio de nutrición", "nutricionista", "nutricionistas", "Lic.", "Lic.", withoutTriage()),
    THERAPY("Centro de terapias", "terapeuta", "terapeutas", "Lic.", "Lic.", withoutTriage()),
    GENERAL("Otro rubro", "profesional", "profesionales", "", "", withoutTriage());

    private final String label;
    private final String professional;
    private final String professionals;
    private final String title;
    private final String titleFemale;
    private final Set<ClinicModule> defaultModules;

    BusinessType(String label, String professional, String professionals, String title, String titleFemale,
                 Set<ClinicModule> defaultModules) {
        this.label = label;
        this.professional = professional;
        this.professionals = professionals;
        this.title = title;
        this.titleFemale = titleFemale;
        this.defaultModules = defaultModules;
    }

    public String label() {
        return label;
    }

    public String professional() {
        return professional;
    }

    public String professionals() {
        return professionals;
    }

    public String title() {
        return title;
    }

    public String titleFemale() {
        return titleFemale;
    }

    public Set<ClinicModule> defaultModules() {
        return EnumSet.copyOf(defaultModules);
    }

    public String professionalLabel() {
        return capitalize(professional);
    }

    public String professionalsLabel() {
        return capitalize(professionals);
    }

    public String titled(String fullName, String gender) {
        String prefix = "F".equalsIgnoreCase(gender) ? titleFemale : title;
        String name = fullName == null ? "" : fullName.trim();
        return prefix.isEmpty() ? name : (prefix + " " + name).trim();
    }

    private static Set<ClinicModule> withoutTriage() {
        Set<ClinicModule> modules = ClinicModule.all();
        modules.remove(ClinicModule.TRIAGE);
        return modules;
    }

    private static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
