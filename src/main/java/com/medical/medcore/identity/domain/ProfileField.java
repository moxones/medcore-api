package com.medical.medcore.identity.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum ProfileField {
    FIRST_NAME("nombre"),
    LAST_NAME("apellido"),
    DOCUMENT("documento de identidad"),
    BIRTH_DATE("fecha de nacimiento"),
    GENDER("sexo"),
    PHONE("teléfono");

    private static final Set<ProfileField> PATIENT_FIELDS = EnumSet.allOf(ProfileField.class);
    private static final Set<ProfileField> STAFF_FIELDS = EnumSet.of(FIRST_NAME, LAST_NAME, DOCUMENT, GENDER, PHONE);
    private static final String PATIENT_ROLE = "PATIENT";

    private final String label;

    ProfileField(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Set<ProfileField> patientFields() {
        return EnumSet.copyOf(PATIENT_FIELDS);
    }

    public static Set<ProfileField> requiredFor(Collection<String> roleCodes) {
        boolean patient = roleCodes != null && roleCodes.contains(PATIENT_ROLE);
        return EnumSet.copyOf(patient ? PATIENT_FIELDS : STAFF_FIELDS);
    }

    public static String describe(List<ProfileField> fields) {
        return String.join(", ", fields.stream().map(ProfileField::label).toList());
    }
}
