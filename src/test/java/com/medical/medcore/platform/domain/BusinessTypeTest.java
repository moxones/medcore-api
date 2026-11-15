package com.medical.medcore.platform.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessTypeTest {

    @Test
    void medicalUsesDoctorTitleByGender() {
        assertThat(BusinessType.MEDICAL.titled("Ana Pérez", "F")).isEqualTo("Dra. Ana Pérez");
        assertThat(BusinessType.MEDICAL.titled("Luis Soto", "M")).isEqualTo("Dr. Luis Soto");
        assertThat(BusinessType.MEDICAL.titled("Luis Soto", null)).isEqualTo("Dr. Luis Soto");
    }

    @Test
    void psychologyUsesItsOwnTermsAndTitle() {
        assertThat(BusinessType.PSYCHOLOGY.professional()).isEqualTo("psicólogo");
        assertThat(BusinessType.PSYCHOLOGY.professionalsLabel()).isEqualTo("Psicólogos");
        assertThat(BusinessType.PSYCHOLOGY.titled("Ana Pérez", "F")).isEqualTo("Ps. Ana Pérez");
    }

    @Test
    void generalHasNoTitle() {
        assertThat(BusinessType.GENERAL.titled(" Ana Pérez ", "F")).isEqualTo("Ana Pérez");
    }

    @Test
    void onlyMedicalEnablesTriageByDefault() {
        assertThat(BusinessType.MEDICAL.defaultModules()).containsExactlyInAnyOrderElementsOf(ClinicModule.all());
        for (BusinessType type : BusinessType.values()) {
            if (type != BusinessType.MEDICAL) {
                assertThat(type.defaultModules()).doesNotContain(ClinicModule.TRIAGE).contains(ClinicModule.CONSULTATION);
            }
        }
    }

    @Test
    void defaultModulesAreDefensiveCopies() {
        BusinessType.PSYCHOLOGY.defaultModules().clear();
        assertThat(BusinessType.PSYCHOLOGY.defaultModules()).isNotEmpty();
    }
}
