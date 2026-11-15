package com.medical.medcore.schedule.service;

import com.medical.medcore.branch.domain.Branch;
import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.doctor.domain.DoctorSchedule;
import com.medical.medcore.schedule.domain.BranchClosure;
import com.medical.medcore.schedule.domain.ExceptionKind;
import com.medical.medcore.schedule.domain.ExceptionReason;
import com.medical.medcore.schedule.domain.ScheduleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkingCalendarTest {

    private static final Long DOCTOR = 1L;
    private static final Long BRANCH = 10L;
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 16);
    private static final Doctor DOCTOR_ENTITY = Doctor.builder().id(DOCTOR).build();
    private static final Branch BRANCH_ENTITY = Branch.builder().id(BRANCH).build();

    @Test
    void subtractSplitsAWindowAroundTheBlock() {
        List<WorkWindow> windows = List.of(window("09:00", "13:00"));

        List<WorkWindow> result = WorkingCalendar.subtract(windows, LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertThat(result).extracting(WorkWindow::start, WorkWindow::end).containsExactly(
                org.assertj.core.groups.Tuple.tuple(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                org.assertj.core.groups.Tuple.tuple(LocalTime.of(11, 0), LocalTime.of(13, 0)));
    }

    @Test
    void subtractRemovesCoveredWindowsAndKeepsOthers() {
        List<WorkWindow> windows = List.of(window("09:00", "11:00"), window("15:00", "18:00"));

        List<WorkWindow> result = WorkingCalendar.subtract(windows, LocalTime.of(8, 0), LocalTime.of(12, 0));

        assertThat(result).extracting(WorkWindow::start).containsExactly(LocalTime.of(15, 0));
    }

    @Test
    void templateAppliesOnItsWeekday() {
        WorkingCalendar.Snapshot snapshot = snapshot(List.of(), List.of());

        assertThat(snapshot.windows(DOCTOR, MONDAY)).hasSize(1);
        assertThat(snapshot.windows(DOCTOR, MONDAY.plusDays(1))).isEmpty();
    }

    @Test
    void fullDayBlockRemovesTheTemplateButKeepsExtraHours() {
        ScheduleException block = exception(ExceptionKind.BLOCK, null, null, null);
        ScheduleException extra = exception(ExceptionKind.EXTRA, "15:00", "18:00", BRANCH_ENTITY);

        List<WorkWindow> windows = snapshot(List.of(block, extra), List.of()).windows(DOCTOR, MONDAY);

        assertThat(windows).hasSize(1);
        assertThat(windows.get(0).source()).isEqualTo(WorkWindow.Source.EXTRA);
        assertThat(windows.get(0).slotMinutes()).isEqualTo(20);
    }

    @Test
    void partialBlockLeavesTheRestOfTheMorning() {
        ScheduleException block = exception(ExceptionKind.BLOCK, "09:00", "11:00", null);

        List<WorkWindow> windows = snapshot(List.of(block), List.of()).windows(DOCTOR, MONDAY);

        assertThat(windows).extracting(WorkWindow::start, WorkWindow::end)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(LocalTime.of(11, 0), LocalTime.of(13, 0)));
    }

    @Test
    void closureRemovesTheTemplateButNotExtraHours() {
        BranchClosure holiday = BranchClosure.builder().closureDate(MONDAY).name("Feriado").build();
        ScheduleException extra = exception(ExceptionKind.EXTRA, "15:00", "17:00", BRANCH_ENTITY);

        WorkingCalendar.Snapshot snapshot = snapshot(List.of(extra), List.of(holiday));

        assertThat(snapshot.closure(MONDAY)).isPresent();
        assertThat(snapshot.windows(DOCTOR, MONDAY)).extracting(WorkWindow::source)
                .containsExactly(WorkWindow.Source.EXTRA);
        assertThat(snapshot.templateWindows(DOCTOR, MONDAY)).hasSize(1);
    }

    @Test
    void scheduleOutsideItsValidityDoesNotApply() {
        DoctorSchedule expired = schedule();
        expired.setValidUntil(MONDAY.minusDays(1));

        WorkingCalendar.Snapshot snapshot = new WorkingCalendar.Snapshot(BRANCH, List.of(expired), List.of(), List.of());

        assertThat(snapshot.windows(DOCTOR, MONDAY)).isEmpty();
    }

    private WorkingCalendar.Snapshot snapshot(List<ScheduleException> exceptions, List<BranchClosure> closures) {
        return new WorkingCalendar.Snapshot(BRANCH, List.of(schedule()), exceptions, closures);
    }

    private DoctorSchedule schedule() {
        return DoctorSchedule.builder()
                .doctor(DOCTOR_ENTITY)
                .dayOfWeek(0)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(13, 0))
                .slotDurationMinutes(20)
                .isActive(true)
                .build();
    }

    private ScheduleException exception(ExceptionKind kind, String start, String end, Branch branch) {
        return ScheduleException.builder()
                .doctor(DOCTOR_ENTITY)
                .branch(branch)
                .kind(kind)
                .startDate(MONDAY)
                .endDate(MONDAY)
                .startTime(start != null ? LocalTime.parse(start) : null)
                .endTime(end != null ? LocalTime.parse(end) : null)
                .reason(kind == ExceptionKind.EXTRA ? ExceptionReason.EXTRA_HOURS : ExceptionReason.VACATION)
                .build();
    }

    private WorkWindow window(String start, String end) {
        return new WorkWindow(LocalTime.parse(start), LocalTime.parse(end), 30, WorkWindow.Source.SCHEDULE, null);
    }
}
