package com.medical.medcore.schedule.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.domain.AppointmentStatusCode;
import com.medical.medcore.appointment.domain.CancellationOrigin;
import com.medical.medcore.appointment.domain.RescheduleCause;
import com.medical.medcore.appointment.repository.AppointmentRepository;
import com.medical.medcore.appointment.service.AppointmentService;
import com.medical.medcore.appointment.service.AppointmentStatuses;
import com.medical.medcore.appointment.service.BookingPolicy;
import com.medical.medcore.platform.service.TenantTerminology;
import com.medical.medcore.schedule.domain.AffectedResolution;
import com.medical.medcore.schedule.domain.BranchClosure;
import com.medical.medcore.schedule.domain.ScheduleException;
import com.medical.medcore.schedule.dto.AffectedAppointmentResponse;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AffectedAppointments {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record Outcome(int flagged, int cancelled) {
        static final Outcome NONE = new Outcome(0, 0);
    }

    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final AppointmentStatuses statuses;
    private final BookingPolicy bookingPolicy;
    private final WorkingCalendar workingCalendar;
    private final TenantTerminology terminology;

    public List<Appointment> blockedBy(ScheduleException exception) {
        LocalDate from = later(exception.getStartDate(), bookingPolicy.today());
        if (from.isAfter(exception.getEndDate())) {
            return List.of();
        }
        return open(exception.getDoctor().getId(), exception.branchId(), from, exception.getEndDate()).stream()
                .filter(appointment -> exception.isFullDay() || overlaps(appointment,
                        exception.getStartTime(), exception.getEndTime()))
                .toList();
    }

    public List<Appointment> bookedInside(ScheduleException extra) {
        LocalDate from = later(extra.getStartDate(), bookingPolicy.today());
        if (from.isAfter(extra.getEndDate())) {
            return List.of();
        }
        return open(extra.getDoctor().getId(), extra.branchId(), from, extra.getEndDate()).stream()
                .filter(appointment -> overlaps(appointment, extra.getStartTime(), extra.getEndTime()))
                .toList();
    }

    public List<Appointment> closedBy(Long branchId, LocalDate date) {
        List<Appointment> candidates = open(null, branchId, date, date);
        return candidates.stream()
                .filter(appointment -> !insideExtraHours(appointment))
                .toList();
    }

    @Transactional
    public Outcome resolveForException(ScheduleException exception, List<Appointment> affected,
                                       AffectedResolution resolution, String message) {
        String note = describe(exception);
        return resolve(affected, resolution, message, note, CancellationOrigin.PROFESSIONAL,
                RescheduleCause.PROFESSIONAL_ABSENCE, exception.getId(), null);
    }

    @Transactional
    public Outcome resolveForClosure(BranchClosure closure, List<Appointment> affected,
                                     AffectedResolution resolution, String message) {
        String note = "Sede cerrada el " + DAY.format(closure.getClosureDate()) + ": " + closure.getName();
        return resolve(affected, resolution, message, note, CancellationOrigin.ORGANIZATION,
                RescheduleCause.BRANCH_CLOSURE, null, closure.getId());
    }

    @Transactional
    public int restoreException(Long exceptionId) {
        List<Appointment> flagged = appointmentRepository.findFlaggedByException(TenantContext.requireTenantId(),
                statuses.idOf(AppointmentStatusCode.SCHEDULED), exceptionId);
        flagged.forEach(Appointment::withdrawRescheduleFlag);
        appointmentRepository.saveAll(flagged);
        return flagged.size();
    }

    @Transactional
    public int restoreClosure(Long closureId) {
        List<Appointment> flagged = appointmentRepository.findFlaggedByClosure(TenantContext.requireTenantId(),
                statuses.idOf(AppointmentStatusCode.SCHEDULED), closureId);
        flagged.forEach(Appointment::withdrawRescheduleFlag);
        appointmentRepository.saveAll(flagged);
        return flagged.size();
    }

    public List<AffectedAppointmentResponse> toResponses(List<Appointment> appointments) {
        return appointments.stream()
                .map(appointment -> new AffectedAppointmentResponse(
                        appointment.getId(),
                        appointment.getScheduledAt(),
                        appointment.getDurationMinutes(),
                        appointment.getPatient().getId(),
                        appointment.getPatient().getPerson().fullName(),
                        appointment.getPatient().getPerson().getPhone(),
                        appointment.getDoctor().getId(),
                        terminology.professionalName(appointment.getDoctor().getPerson()),
                        appointment.getBranch() != null ? appointment.getBranch().getId() : null,
                        appointment.getBranch() != null ? appointment.getBranch().getName() : null,
                        appointment.getFlowStatus(),
                        appointment.isFlaggedForReschedule()))
                .toList();
    }

    public String describe(ScheduleException exception) {
        String range = exception.getStartDate().equals(exception.getEndDate())
                ? "el " + DAY.format(exception.getStartDate())
                : "del " + DAY.format(exception.getStartDate()) + " al " + DAY.format(exception.getEndDate());
        String hours = exception.isFullDay() ? "" : " de " + exception.getStartTime() + " a " + exception.getEndTime();
        String kind = exception.isBlock()
                ? "Ausencia del profesional (" + exception.getReason().label() + ") "
                : "Se anularon las horas extra ";
        return kind + range + hours;
    }

    private Outcome resolve(List<Appointment> affected, AffectedResolution resolution, String message, String note,
                            CancellationOrigin origin, RescheduleCause cause, Long exceptionId, Long closureId) {
        if (affected.isEmpty()) {
            return Outcome.NONE;
        }
        if (resolution == AffectedResolution.CANCEL) {
            String reason = message != null && !message.isBlank() ? message.trim() : note;
            for (Appointment appointment : affected) {
                appointment.flagForReschedule(cause, note, exceptionId, closureId, bookingPolicy.now());
                appointmentService.cancelBySchedule(appointment, reason, origin);
            }
            return new Outcome(0, affected.size());
        }
        for (Appointment appointment : affected) {
            appointment.flagForReschedule(cause, note, exceptionId, closureId, bookingPolicy.now());
        }
        appointmentRepository.saveAll(affected);
        return new Outcome(affected.size(), 0);
    }

    private List<Appointment> open(Long doctorId, Long branchId, LocalDate from, LocalDate to) {
        return appointmentRepository.findOpenInRange(TenantContext.requireTenantId(), doctorId, branchId,
                from.atStartOfDay(), to.plusDays(1).atStartOfDay(), statuses.idOf(AppointmentStatusCode.SCHEDULED));
    }

    private boolean insideExtraHours(Appointment appointment) {
        if (appointment.getBranch() == null) {
            return false;
        }
        LocalDate date = appointment.getScheduledAt().toLocalDate();
        LocalTime start = appointment.getScheduledAt().toLocalTime();
        LocalTime end = appointment.endsAt().toLocalTime();
        return workingCalendar.windows(appointment.getDoctor().getId(), appointment.getBranch().getId(), date).stream()
                .filter(window -> window.source() == WorkWindow.Source.EXTRA)
                .anyMatch(window -> window.contains(start, end));
    }

    private static boolean overlaps(Appointment appointment, LocalTime start, LocalTime end) {
        LocalTime appointmentStart = appointment.getScheduledAt().toLocalTime();
        LocalTime appointmentEnd = appointment.endsAt().toLocalTime();
        return appointmentStart.isBefore(end) && start.isBefore(appointmentEnd);
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
