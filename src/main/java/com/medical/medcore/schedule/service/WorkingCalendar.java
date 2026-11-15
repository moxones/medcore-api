package com.medical.medcore.schedule.service;

import com.medical.medcore.doctor.domain.DoctorSchedule;
import com.medical.medcore.doctor.repository.DoctorScheduleRepository;
import com.medical.medcore.schedule.domain.BranchClosure;
import com.medical.medcore.schedule.domain.ExceptionKind;
import com.medical.medcore.schedule.domain.ScheduleException;
import com.medical.medcore.schedule.repository.BranchClosureRepository;
import com.medical.medcore.schedule.repository.ScheduleExceptionRepository;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkingCalendar {

    public static final int DEFAULT_SLOT_MINUTES = 30;

    private final DoctorScheduleRepository scheduleRepository;
    private final ScheduleExceptionRepository exceptionRepository;
    private final BranchClosureRepository closureRepository;

    public Snapshot load(Collection<Long> doctorIds, Long branchId, LocalDate from, LocalDate to) {
        if (doctorIds.isEmpty()) {
            return new Snapshot(branchId, List.of(), List.of(), List.of());
        }
        List<Long> ids = List.copyOf(doctorIds);
        return new Snapshot(branchId,
                scheduleRepository.findActiveByDoctorIdInAndBranchId(ids, branchId),
                exceptionRepository.findApprovedInRange(ids, from, to).stream()
                        .filter(exception -> exception.appliesToBranch(branchId))
                        .toList(),
                closureRepository.findInRange(TenantContext.requireTenantId(), branchId, from, to));
    }

    public List<WorkWindow> windows(Long doctorId, Long branchId, LocalDate date) {
        return load(List.of(doctorId), branchId, date, date).windows(doctorId, date);
    }

    static List<WorkWindow> subtract(List<WorkWindow> windows, LocalTime start, LocalTime end) {
        List<WorkWindow> result = new ArrayList<>();
        for (WorkWindow window : windows) {
            boolean overlaps = start.isBefore(window.end()) && window.start().isBefore(end);
            if (!overlaps) {
                result.add(window);
                continue;
            }
            if (window.start().isBefore(start)) {
                result.add(window.withBounds(window.start(), start));
            }
            if (end.isBefore(window.end())) {
                result.add(window.withBounds(end, window.end()));
            }
        }
        return result;
    }

    public static final class Snapshot {

        private final Long branchId;
        private final Map<Long, Map<Integer, List<DoctorSchedule>>> templates = new HashMap<>();
        private final Map<Long, List<ScheduleException>> exceptions = new HashMap<>();
        private final Map<LocalDate, BranchClosure> closures = new HashMap<>();

        Snapshot(Long branchId, List<DoctorSchedule> schedules, List<ScheduleException> approved,
                 List<BranchClosure> closureList) {
            this.branchId = branchId;
            for (DoctorSchedule schedule : schedules) {
                templates.computeIfAbsent(schedule.getDoctor().getId(), key -> new HashMap<>())
                        .computeIfAbsent(schedule.getDayOfWeek(), key -> new ArrayList<>())
                        .add(schedule);
            }
            for (ScheduleException exception : approved) {
                exceptions.computeIfAbsent(exception.getDoctor().getId(), key -> new ArrayList<>()).add(exception);
            }
            for (BranchClosure closure : closureList) {
                closures.merge(closure.getClosureDate(), closure,
                        (current, candidate) -> current.getBranch() != null ? current : candidate);
            }
        }

        public Long branchId() {
            return branchId;
        }

        public Optional<BranchClosure> closure(LocalDate date) {
            return Optional.ofNullable(closures.get(date));
        }

        public List<WorkWindow> templateWindows(Long doctorId, LocalDate date) {
            int day = date.getDayOfWeek().getValue() - 1;
            List<WorkWindow> windows = new ArrayList<>();
            for (DoctorSchedule schedule : templates.getOrDefault(doctorId, Map.of()).getOrDefault(day, List.of())) {
                boolean started = schedule.getValidFrom() == null || !date.isBefore(schedule.getValidFrom());
                boolean notEnded = schedule.getValidUntil() == null || !date.isAfter(schedule.getValidUntil());
                if (started && notEnded) {
                    windows.add(new WorkWindow(schedule.getStartTime(), schedule.getEndTime(),
                            slotOf(schedule), WorkWindow.Source.SCHEDULE, null));
                }
            }
            windows.sort(Comparator.comparing(WorkWindow::start));
            return windows;
        }

        public List<ScheduleException> exceptions(Long doctorId, LocalDate date) {
            return exceptions.getOrDefault(doctorId, List.of()).stream()
                    .filter(exception -> exception.covers(date))
                    .toList();
        }

        public List<WorkWindow> windows(Long doctorId, LocalDate date) {
            List<WorkWindow> windows = closure(date).isPresent() ? new ArrayList<>() : templateWindows(doctorId, date);
            List<ScheduleException> today = exceptions(doctorId, date);
            for (ScheduleException exception : today) {
                if (exception.getKind() != ExceptionKind.BLOCK) {
                    continue;
                }
                if (exception.isFullDay()) {
                    windows = new ArrayList<>();
                    break;
                }
                windows = subtract(windows, exception.getStartTime(), exception.getEndTime());
            }
            List<WorkWindow> result = new ArrayList<>(windows);
            for (ScheduleException exception : today) {
                if (exception.getKind() == ExceptionKind.EXTRA) {
                    int slot = exception.getSlotDurationMinutes() != null && exception.getSlotDurationMinutes() > 0
                            ? exception.getSlotDurationMinutes()
                            : defaultSlot(doctorId);
                    result.add(new WorkWindow(exception.getStartTime(), exception.getEndTime(), slot,
                            WorkWindow.Source.EXTRA, exception.getId()));
                }
            }
            result.sort(Comparator.comparing(WorkWindow::start));
            return result;
        }

        public int defaultSlot(Long doctorId) {
            return templates.getOrDefault(doctorId, Map.of()).values().stream()
                    .flatMap(List::stream)
                    .map(Snapshot::slotOf)
                    .findFirst()
                    .orElse(DEFAULT_SLOT_MINUTES);
        }

        private static int slotOf(DoctorSchedule schedule) {
            Integer configured = schedule.getSlotDurationMinutes();
            return configured != null && configured > 0 ? configured : DEFAULT_SLOT_MINUTES;
        }
    }
}
