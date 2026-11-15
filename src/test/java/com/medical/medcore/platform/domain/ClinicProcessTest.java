package com.medical.medcore.platform.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicProcessTest {

    @Test
    void triageAndPaymentNeedTheirModule() {
        Set<ClinicModule> none = EnumSet.noneOf(ClinicModule.class);

        assertThat(ClinicProcess.TRIAGE.availableWith(none)).isFalse();
        assertThat(ClinicProcess.PAYMENT.availableWith(none)).isFalse();
        assertThat(ClinicProcess.TRIAGE.availableWith(EnumSet.of(ClinicModule.TRIAGE))).isTrue();
        assertThat(ClinicProcess.PAYMENT.availableWith(EnumSet.of(ClinicModule.BILLING))).isTrue();
    }

    @Test
    void calledNeverDependsOnAModule() {
        assertThat(ClinicProcess.CALLED.availableWith(EnumSet.noneOf(ClinicModule.class))).isTrue();
    }

    @Test
    void triageRequiresConsultation() {
        assertThat(ClinicModule.TRIAGE.requires()).containsExactly(ClinicModule.CONSULTATION);
        assertThat(ClinicModule.BILLING.requires()).isEmpty();
    }

    @Test
    void absenceApprovalIsOptInAndNeedsNoModule() {
        assertThat(ClinicProcess.ABSENCE_APPROVAL.activeByDefault()).isFalse();
        assertThat(ClinicProcess.TRIAGE.activeByDefault()).isTrue();
        assertThat(ClinicProcess.ABSENCE_APPROVAL.availableWith(EnumSet.noneOf(ClinicModule.class))).isTrue();
    }
}
