package com.medical.medcore.appointment.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentFlowStatusTest {

    @ParameterizedTest
    @CsvSource({
            "SCHEDULED, WAITING",
            "WAITING, CALLED",
            "WAITING, IN_PROCESS",
            "CALLED, IN_PROCESS",
            "CALLED, WAITING",
            "IN_PROCESS, PENDING_PAYMENT",
            "IN_PROCESS, COMPLETED",
            "IN_PROCESS, CALLED",
            "PENDING_PAYMENT, COMPLETED",
            "PENDING_PAYMENT, IN_PROCESS"
    })
    void allowsForwardStepsShortcutsAndSingleStepRollbacks(AppointmentFlowStatus from, AppointmentFlowStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "SCHEDULED, CALLED",
            "SCHEDULED, IN_PROCESS",
            "SCHEDULED, COMPLETED",
            "WAITING, SCHEDULED",
            "WAITING, PENDING_PAYMENT",
            "WAITING, COMPLETED",
            "CALLED, COMPLETED",
            "PENDING_PAYMENT, WAITING"
    })
    void rejectsSkippedOrBackwardJumps(AppointmentFlowStatus from, AppointmentFlowStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AppointmentFlowStatus.class)
    void completedIsTerminal(AppointmentFlowStatus target) {
        assertThat(AppointmentFlowStatus.COMPLETED.canTransitionTo(target)).isFalse();
    }

    @Test
    void parsesRawValuesTreatingBlankAsScheduled() {
        assertThat(AppointmentFlowStatus.from(null)).isEqualTo(AppointmentFlowStatus.SCHEDULED);
        assertThat(AppointmentFlowStatus.from("  ")).isEqualTo(AppointmentFlowStatus.SCHEDULED);
        assertThat(AppointmentFlowStatus.from(" in_process ")).isEqualTo(AppointmentFlowStatus.IN_PROCESS);
        assertThatThrownBy(() -> AppointmentFlowStatus.from("FLYING")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void classifiesStagesAroundTheConsultation() {
        assertThat(AppointmentFlowStatus.CALLED.isBeforeConsultation()).isTrue();
        assertThat(AppointmentFlowStatus.IN_PROCESS.isBeforeConsultation()).isFalse();
        assertThat(AppointmentFlowStatus.PENDING_PAYMENT.isAttended()).isTrue();
        assertThat(AppointmentFlowStatus.IN_PROCESS.isAttended()).isFalse();
    }
}
