package com.medical.medcore.appointment.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CareStageTest {

    @ParameterizedTest
    @CsvSource({
            "SCHEDULED, true, false, BOOKED",
            "WAITING, true, false, IN_RECEPTION",
            "WAITING, true, true, READY",
            "WAITING, false, false, READY",
            "CALLED, true, false, IN_RECEPTION",
            "CALLED, true, true, READY",
            "IN_PROCESS, true, true, IN_CONSULTATION",
            "PENDING_PAYMENT, true, true, ATTENDED",
            "COMPLETED, false, false, ATTENDED"
    })
    void resolvesWhereThePatientIs(AppointmentFlowStatus flow, boolean triageEnabled, boolean triageCompleted,
                                   CareStage expected) {
        assertThat(CareStage.resolve(flow, triageEnabled, triageCompleted)).isEqualTo(expected);
    }
}
