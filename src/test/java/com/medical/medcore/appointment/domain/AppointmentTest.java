package com.medical.medcore.appointment.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    private static final LocalDateTime FIRST = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final LocalDateTime LATER = FIRST.plusMinutes(20);

    @Test
    void stampsEachTransitionOnlyTheFirstTime() {
        Appointment appointment = Appointment.builder().scheduledAt(FIRST).build();

        appointment.moveTo(AppointmentFlowStatus.WAITING, FIRST);
        appointment.moveTo(AppointmentFlowStatus.CALLED, FIRST.plusMinutes(5));
        appointment.moveTo(AppointmentFlowStatus.WAITING, LATER);

        assertThat(appointment.getCheckedInAt()).isEqualTo(FIRST);
        assertThat(appointment.getCalledAt()).isEqualTo(FIRST.plusMinutes(5));
        assertThat(appointment.flow()).isEqualTo(AppointmentFlowStatus.WAITING);
    }

    @Test
    void completingDirectlyAlsoStampsTheEndOfTheConsultation() {
        Appointment appointment = Appointment.builder().scheduledAt(FIRST).build();

        appointment.moveTo(AppointmentFlowStatus.COMPLETED, LATER);

        assertThat(appointment.getFinishedAt()).isEqualTo(LATER);
        assertThat(appointment.getCompletedAt()).isEqualTo(LATER);
    }

    @Test
    void scheduledIsNotAReachableTarget() {
        Appointment appointment = Appointment.builder().scheduledAt(FIRST).build();

        assertThatThrownBy(() -> appointment.moveTo(AppointmentFlowStatus.SCHEDULED, LATER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancellingKeepsTheVisitReasonAndRecordsTheAuditTrail() {
        Appointment appointment = Appointment.builder().scheduledAt(FIRST).reason("Control anual").statusId(1L).build();

        appointment.cancel(2L, "Viaje imprevisto", CancellationOrigin.PATIENT, 77L, LATER);

        assertThat(appointment.getReason()).isEqualTo("Control anual");
        assertThat(appointment.getCancelReason()).isEqualTo("Viaje imprevisto");
        assertThat(appointment.getCancelledBy()).isEqualTo(77L);
        assertThat(appointment.getCancelledAt()).isEqualTo(LATER);
        assertThat(appointment.getStatusId()).isEqualTo(2L);
        assertThat(appointment.getCancellationOrigin()).isEqualTo(CancellationOrigin.PATIENT);
    }

    @Test
    void computesTheEndFromTheDuration() {
        Appointment appointment = Appointment.builder().scheduledAt(FIRST).durationMinutes(45).build();

        assertThat(appointment.endsAt()).isEqualTo(FIRST.plusMinutes(45));
    }

    @Test
    void flaggedAppointmentCancelledByClosureBelongsToTheOrganization() {
        Appointment appointment = Appointment.builder().scheduledAt(FIRST).statusId(1L).build();

        appointment.flagForReschedule(RescheduleCause.BRANCH_CLOSURE, "Feriado", null, 9L, LATER);

        assertThat(appointment.isFlaggedForReschedule()).isTrue();
        assertThat(appointment.defaultCancellationOrigin()).isEqualTo(CancellationOrigin.ORGANIZATION);
        appointment.cancel(2L, "Feriado", appointment.defaultCancellationOrigin(), 77L, LATER);
        assertThat(appointment.isFlaggedForReschedule()).isFalse();
        assertThat(appointment.getBranchClosureId()).isEqualTo(9L);
    }

    @Test
    void withdrawingTheFlagForgetsTheCause() {
        Appointment appointment = Appointment.builder().scheduledAt(FIRST).statusId(1L).build();
        appointment.flagForReschedule(RescheduleCause.PROFESSIONAL_ABSENCE, "Vacaciones", 4L, null, LATER);

        appointment.withdrawRescheduleFlag();

        assertThat(appointment.isFlaggedForReschedule()).isFalse();
        assertThat(appointment.getScheduleExceptionId()).isNull();
        assertThat(appointment.getRescheduleCause()).isNull();
        assertThat(appointment.defaultCancellationOrigin()).isEqualTo(CancellationOrigin.PATIENT);
    }

    @Test
    void returningToScheduleClearsTheArrival() {
        Appointment appointment = Appointment.builder().scheduledAt(FIRST).statusId(1L).build();
        appointment.moveTo(AppointmentFlowStatus.WAITING, LATER);

        appointment.returnToSchedule();

        assertThat(appointment.flow()).isEqualTo(AppointmentFlowStatus.SCHEDULED);
        assertThat(appointment.getCheckedInAt()).isNull();
    }
}
