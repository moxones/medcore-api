package com.medical.medcore.schedule.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.domain.AppointmentStatusCode;
import com.medical.medcore.appointment.repository.AppointmentRepository;
import com.medical.medcore.appointment.service.AppointmentStatuses;
import com.medical.medcore.appointment.service.BookingPolicy;
import com.medical.medcore.branch.domain.Branch;
import com.medical.medcore.branch.repository.BranchRepository;
import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.doctor.domain.DoctorBranch;
import com.medical.medcore.doctor.repository.DoctorBranchRepository;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.platform.service.TenantTerminology;
import com.medical.medcore.schedule.domain.AffectedResolution;
import com.medical.medcore.schedule.domain.BranchClosure;
import com.medical.medcore.schedule.domain.ExceptionKind;
import com.medical.medcore.schedule.domain.ExceptionReason;
import com.medical.medcore.schedule.domain.ExceptionStatus;
import com.medical.medcore.schedule.domain.ScheduleException;
import com.medical.medcore.schedule.dto.CalendarBranchResponse;
import com.medical.medcore.schedule.dto.CalendarDayResponse;
import com.medical.medcore.schedule.dto.CalendarWindowResponse;
import com.medical.medcore.schedule.dto.DoctorCalendarResponse;
import com.medical.medcore.schedule.dto.ExceptionOutcomeResponse;
import com.medical.medcore.schedule.dto.ImpactPreviewResponse;
import com.medical.medcore.schedule.dto.ResolutionRequest;
import com.medical.medcore.schedule.dto.ScheduleExceptionRequest;
import com.medical.medcore.schedule.dto.ScheduleExceptionResponse;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.SecurityUtils;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleExceptionService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] WEEKDAYS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final int MAX_BLOCK_DAYS = 366;
    private static final int MAX_EXTRA_DAYS = 92;
    private static final int MAX_CALENDAR_DAYS = 62;
    private static final int MAX_LIST_DAYS = 400;

    private final ScheduleAccess access;
    private final WorkingCalendar workingCalendar;
    private final AffectedAppointments affectedAppointments;
    private final com.medical.medcore.schedule.repository.ScheduleExceptionRepository exceptionRepository;
    private final com.medical.medcore.schedule.repository.BranchClosureRepository closureRepository;
    private final BranchRepository branchRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatuses statuses;
    private final UserRepository userRepository;
    private final BookingPolicy bookingPolicy;
    private final TenantTerminology terminology;

    public List<ScheduleExceptionResponse> list(Long doctorId, LocalDate from, LocalDate to, String status) {
        LocalDate start = from != null ? from : bookingPolicy.today();
        LocalDate end = to != null ? to : start.plusDays(90);
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > MAX_LIST_DAYS) {
            throw new BadRequestException("El rango de fechas es inválido");
        }
        Long effectiveDoctor = doctorId;
        if (access.actsAsOwnProfessional()) {
            Long own = access.ownDoctorId().orElseThrow(() -> new ForbiddenException("NO_DOCTOR_PROFILE",
                    "Tu usuario no tiene un perfil profesional"));
            if (doctorId != null && !doctorId.equals(own)) {
                throw new ForbiddenException("Solo puedes gestionar tu propia agenda");
            }
            effectiveDoctor = own;
        } else if (doctorId != null) {
            access.requireReadable(doctorId);
        }
        List<ScheduleException> found = exceptionRepository.search(TenantContext.requireTenantId(), effectiveDoctor,
                parseStatus(status), start, end);
        Map<Long, Boolean> visible = new HashMap<>();
        List<ScheduleException> allowed = found.stream()
                .filter(exception -> visible.computeIfAbsent(exception.getDoctor().getId(),
                        access::visibleToBranchScope))
                .toList();
        return toResponses(allowed);
    }

    public List<ScheduleExceptionResponse> pending() {
        if (!SecurityUtils.isAdmin()) {
            throw new ForbiddenException("Solo la administración revisa solicitudes");
        }
        return toResponses(exceptionRepository.findPending(TenantContext.requireTenantId()));
    }

    public ImpactPreviewResponse preview(ScheduleExceptionRequest request) {
        ScheduleException draft = draft(request, null);
        boolean approval = access.requiresApproval();
        List<Appointment> affected = draft.isBlock() ? affectedAppointments.blockedBy(draft) : List.of();
        List<String> warnings = new ArrayList<>();
        if (approval) {
            warnings.add("La administración debe aprobar esta solicitud antes de que se aplique.");
        }
        if (draft.isBlock() && !affected.isEmpty() && approval) {
            warnings.add("Las citas afectadas se marcarán para reprogramar cuando se apruebe.");
        }
        if (draft.isBlock() && hasNoTemplate(draft)) {
            warnings.add("El profesional no tiene horario habitual en esas fechas: la ausencia no cambia su disponibilidad.");
        }
        return new ImpactPreviewResponse(affected.size(), affectedAppointments.toResponses(affected), approval,
                !access.actsAsOwnProfessional(), warnings);
    }

    @Transactional
    public ExceptionOutcomeResponse create(ScheduleExceptionRequest request) {
        ScheduleException exception = draft(request, null);
        boolean approval = access.requiresApproval();
        exception.setStatus(approval ? ExceptionStatus.PENDING : ExceptionStatus.APPROVED);
        exception = exceptionRepository.saveAndFlush(exception);
        AffectedAppointments.Outcome outcome = AffectedAppointments.Outcome.NONE;
        if (!approval && exception.isBlock()) {
            outcome = affectedAppointments.resolveForException(exception, affectedAppointments.blockedBy(exception),
                    resolutionFor(request.resolution()), request.cancelMessage());
        }
        return new ExceptionOutcomeResponse(single(exception), outcome.flagged(), outcome.cancelled(), 0);
    }

    public ImpactPreviewResponse cancelPreview(Long id) {
        ScheduleException exception = requireOpen(id);
        access.assertManageable(exception.getDoctor());
        List<Appointment> affected = exception.isApproved() && !exception.isBlock()
                ? affectedAppointments.bookedInside(exception)
                : List.of();
        List<String> warnings = new ArrayList<>();
        if (exception.isApproved() && exception.isBlock()) {
            long pending = pendingFor(exception.getId());
            if (pending > 0) {
                warnings.add(pending + " cita(s) marcadas para reprogramar volverán a su estado normal.");
            }
        }
        return new ImpactPreviewResponse(affected.size(), affectedAppointments.toResponses(affected), false,
                !access.actsAsOwnProfessional(), warnings);
    }

    @Transactional
    public ExceptionOutcomeResponse cancel(Long id, ResolutionRequest request) {
        ScheduleException exception = requireOpen(id);
        access.assertManageable(exception.getDoctor());
        if (exception.getEndDate().isBefore(bookingPolicy.today())) {
            throw new ConflictException("EXCEPTION_PAST", "La excepción ya terminó y no se puede anular");
        }
        int restored = 0;
        AffectedAppointments.Outcome outcome = AffectedAppointments.Outcome.NONE;
        if (exception.isApproved()) {
            if (exception.isBlock()) {
                restored = affectedAppointments.restoreException(exception.getId());
            } else {
                List<Appointment> inside = affectedAppointments.bookedInside(exception);
                outcome = affectedAppointments.resolveForException(exception, inside,
                        resolutionFor(request != null ? request.resolution() : null),
                        request != null ? request.message() : null);
            }
        }
        exception.cancel(TenantContext.getCurrentUserId(), bookingPolicy.now());
        exceptionRepository.save(exception);
        return new ExceptionOutcomeResponse(single(exception), outcome.flagged(), outcome.cancelled(), restored);
    }

    @Transactional
    public ExceptionOutcomeResponse approve(Long id, String note) {
        ScheduleException exception = requirePending(id);
        validateOverlaps(exception, exception.getId());
        if (exception.getKind() == ExceptionKind.EXTRA) {
            validateAgainstTemplate(exception);
        }
        exception.review(ExceptionStatus.APPROVED, TenantContext.getCurrentUserId(), clean(note), bookingPolicy.now());
        exception = exceptionRepository.saveAndFlush(exception);
        AffectedAppointments.Outcome outcome = AffectedAppointments.Outcome.NONE;
        if (exception.isBlock()) {
            outcome = affectedAppointments.resolveForException(exception, affectedAppointments.blockedBy(exception),
                    AffectedResolution.FLAG, null);
        }
        return new ExceptionOutcomeResponse(single(exception), outcome.flagged(), outcome.cancelled(), 0);
    }

    @Transactional
    public ExceptionOutcomeResponse reject(Long id, String note) {
        ScheduleException exception = requirePending(id);
        exception.review(ExceptionStatus.REJECTED, TenantContext.getCurrentUserId(), clean(note), bookingPolicy.now());
        exceptionRepository.save(exception);
        return new ExceptionOutcomeResponse(single(exception), 0, 0, 0);
    }

    public DoctorCalendarResponse calendar(Long doctorId, LocalDate from, LocalDate to) {
        Doctor doctor = access.requireReadable(doctorId);
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_CALENDAR_DAYS) {
            throw new BadRequestException("El rango máximo del calendario es de " + MAX_CALENDAR_DAYS + " días");
        }
        Long tenantId = TenantContext.requireTenantId();
        List<DoctorBranch> links = doctorBranchRepository.findActiveBranchesByDoctorId(doctorId);
        Map<Long, WorkingCalendar.Snapshot> snapshots = new LinkedHashMap<>();
        Map<Long, String> branchNames = new HashMap<>();
        for (DoctorBranch link : links) {
            Branch branch = link.getBranch();
            branchNames.put(branch.getId(), branch.getName());
            snapshots.put(branch.getId(), workingCalendar.load(List.of(doctorId), branch.getId(), from, to));
        }
        List<ScheduleException> exceptions = exceptionRepository.search(tenantId, doctorId, null, from, to).stream()
                .filter(exception -> exception.getStatus().isOpen())
                .toList();
        List<BranchClosure> closures = closureRepository.findInRange(tenantId, null, from, to);
        Map<LocalDate, int[]> appointmentCounts = new HashMap<>();
        for (Appointment appointment : appointmentRepository.findByDoctorAndDate(tenantId, doctorId,
                from.atStartOfDay(), to.plusDays(1).atStartOfDay(), statuses.inactiveIds())) {
            int[] counts = appointmentCounts.computeIfAbsent(appointment.getScheduledAt().toLocalDate(),
                    key -> new int[2]);
            counts[0]++;
            if (appointment.isFlaggedForReschedule()) {
                counts[1]++;
            }
        }

        List<CalendarDayResponse> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<CalendarWindowResponse> windows = new ArrayList<>();
            long templateMinutes = 0;
            long effectiveMinutes = 0;
            for (Map.Entry<Long, WorkingCalendar.Snapshot> entry : snapshots.entrySet()) {
                WorkingCalendar.Snapshot snapshot = entry.getValue();
                templateMinutes += snapshot.templateWindows(doctorId, date).stream().mapToLong(WorkWindow::minutes).sum();
                for (WorkWindow window : snapshot.windows(doctorId, date)) {
                    effectiveMinutes += window.minutes();
                    windows.add(new CalendarWindowResponse(entry.getKey(), branchNames.get(entry.getKey()),
                            window.start().format(TIME), window.end().format(TIME), window.source().name()));
                }
            }
            windows.sort(Comparator.comparing(CalendarWindowResponse::startTime));
            LocalDate day = date;
            List<String> closureNames = closures.stream()
                    .filter(closure -> closure.getClosureDate().equals(day))
                    .filter(closure -> closure.getBranch() == null || branchNames.containsKey(closure.branchId()))
                    .map(closure -> closure.getBranch() == null
                            ? closure.getName()
                            : closure.getBranch().getName() + ": " + closure.getName())
                    .toList();
            List<Long> exceptionIds = exceptions.stream()
                    .filter(exception -> exception.covers(day))
                    .map(ScheduleException::getId)
                    .toList();
            int[] counts = appointmentCounts.getOrDefault(date, new int[2]);
            days.add(new CalendarDayResponse(date, windows, templateMinutes, effectiveMinutes, closureNames,
                    exceptionIds, counts[0], counts[1]));
        }
        List<CalendarBranchResponse> branches = branchNames.entrySet().stream()
                .map(entry -> new CalendarBranchResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CalendarBranchResponse::branchName))
                .toList();
        return new DoctorCalendarResponse(doctor.getId(), terminology.professionalName(doctor.getPerson()), branches,
                days, toResponses(exceptions));
    }

    private ScheduleException draft(ScheduleExceptionRequest request, Long excludeId) {
        Doctor doctor = access.requireManageable(request.doctorId());
        LocalDate today = bookingPolicy.today();
        LocalDate start = request.startDate();
        LocalDate end = request.endDate() != null ? request.endDate() : start;
        if (end.isBefore(start)) {
            throw new BadRequestException("INVALID_RANGE", "La fecha final debe ser igual o posterior a la inicial");
        }
        if (end.isBefore(today)) {
            throw new BadRequestException("PAST_DATES", "No se pueden registrar cambios en fechas pasadas");
        }
        ExceptionKind kind = request.kind();
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        int maxDays = kind == ExceptionKind.BLOCK ? MAX_BLOCK_DAYS : MAX_EXTRA_DAYS;
        if (days > maxDays) {
            throw new BadRequestException("INVALID_RANGE", "El rango máximo es de " + maxDays + " días");
        }
        if (kind == ExceptionKind.EXTRA && start.isBefore(today)) {
            throw new BadRequestException("PAST_DATES", "Las horas extra deben empezar hoy o después");
        }
        LocalTime startTime = request.startTime();
        LocalTime endTime = request.endTime();
        if ((startTime == null) != (endTime == null)) {
            throw new BadRequestException("INVALID_TIMES", "Indica la hora de inicio y la de fin, o ninguna para todo el día");
        }
        if (startTime != null && !startTime.isBefore(endTime)) {
            throw new BadRequestException("INVALID_TIMES", "La hora de inicio debe ser anterior a la hora de fin");
        }
        if (kind == ExceptionKind.EXTRA && startTime == null) {
            throw new BadRequestException("INVALID_TIMES", "Las horas extra necesitan hora de inicio y de fin");
        }
        if (kind == ExceptionKind.EXTRA && request.branchId() == null) {
            throw new BadRequestException("BRANCH_REQUIRED", "Indica la sede de las horas extra");
        }
        Branch branch = null;
        if (request.branchId() != null) {
            branch = branchRepository.findByIdAndTenantId(request.branchId(), TenantContext.requireTenantId())
                    .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));
            if (!doctorBranchRepository.existsByDoctor_IdAndBranch_IdAndIsActiveTrue(doctor.getId(), branch.getId())) {
                throw new BadRequestException("El profesional no atiende en la sucursal seleccionada");
            }
        }
        ExceptionReason reason = resolveReason(kind, request.reason());
        Integer slot = kind == ExceptionKind.EXTRA ? request.slotDurationMinutes() : null;
        ScheduleException draft = ScheduleException.builder()
                .doctor(doctor)
                .branch(branch)
                .kind(kind)
                .startDate(start)
                .endDate(end)
                .startTime(startTime)
                .endTime(endTime)
                .slotDurationMinutes(slot)
                .reason(reason)
                .note(clean(request.note()))
                .build();
        validateOverlaps(draft, excludeId);
        if (kind == ExceptionKind.EXTRA) {
            validateAgainstTemplate(draft);
        }
        return draft;
    }

    private void validateOverlaps(ScheduleException candidate, Long excludeId) {
        for (ScheduleException other : exceptionRepository.findOpenOverlapping(candidate.getDoctor().getId(),
                candidate.getStartDate(), candidate.getEndDate(), excludeId)) {
            boolean branchesMeet = candidate.getBranch() == null || other.getBranch() == null
                    || candidate.branchId().equals(other.branchId());
            boolean timesMeet = candidate.isFullDay() || other.isFullDay()
                    || other.overlapsTime(candidate.getStartTime(), candidate.getEndTime());
            if (!timesMeet) {
                continue;
            }
            String when = describeDates(other);
            if (candidate.isBlock() && other.isBlock() && branchesMeet) {
                throw new ConflictException("EXCEPTION_OVERLAP", "Ya hay una ausencia registrada que se cruza (" + when + ")");
            }
            if (candidate.isBlock() && !other.isBlock() && branchesMeet) {
                throw new ConflictException("OVERLAPS_EXTRA",
                        "Se cruza con horas extra registradas (" + when + "). Anúlalas primero.");
            }
            if (!candidate.isBlock() && !other.isBlock()) {
                throw new ConflictException("EXCEPTION_OVERLAP", "Ya hay horas extra que se cruzan (" + when + ")");
            }
        }
    }

    private void validateAgainstTemplate(ScheduleException extra) {
        Long doctorId = extra.getDoctor().getId();
        Set<Long> branches = access.branchIdsOf(doctorId);
        for (Long branchId : branches) {
            WorkingCalendar.Snapshot snapshot = workingCalendar.load(List.of(doctorId), branchId,
                    extra.getStartDate(), extra.getEndDate());
            for (LocalDate date = extra.getStartDate(); !date.isAfter(extra.getEndDate()); date = date.plusDays(1)) {
                for (WorkWindow window : snapshot.windows(doctorId, date)) {
                    if (window.source() != WorkWindow.Source.SCHEDULE) {
                        continue;
                    }
                    if (window.start().isBefore(extra.getEndTime()) && extra.getStartTime().isBefore(window.end())) {
                        throw new ConflictException("OVERLAPS_SCHEDULE",
                                "Ese horario ya es parte de su atención habitual del " + WEEKDAYS[date.getDayOfWeek().getValue() - 1]
                                        + " " + DAY.format(date) + " (" + window.start().format(TIME) + "–"
                                        + window.end().format(TIME) + ")");
                    }
                }
            }
        }
    }

    private boolean hasNoTemplate(ScheduleException block) {
        Long doctorId = block.getDoctor().getId();
        Set<Long> branches = block.getBranch() != null ? Set.of(block.branchId()) : access.branchIdsOf(doctorId);
        LocalDate last = block.getStartDate().plusDays(Math.min(block.days(), 14) - 1);
        for (Long branchId : branches) {
            WorkingCalendar.Snapshot snapshot = workingCalendar.load(List.of(doctorId), branchId, block.getStartDate(), last);
            for (LocalDate date = block.getStartDate(); !date.isAfter(last); date = date.plusDays(1)) {
                if (!snapshot.templateWindows(doctorId, date).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private ScheduleException requireOpen(Long id) {
        ScheduleException exception = exceptionRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Excepción no encontrada"));
        if (!exception.getStatus().isOpen()) {
            throw new ConflictException("EXCEPTION_CLOSED", "La excepción ya fue anulada o rechazada");
        }
        return exception;
    }

    private ScheduleException requirePending(Long id) {
        if (!SecurityUtils.isAdmin()) {
            throw new ForbiddenException("Solo la administración revisa solicitudes");
        }
        ScheduleException exception = requireOpen(id);
        if (exception.getStatus() != ExceptionStatus.PENDING) {
            throw new ConflictException("EXCEPTION_NOT_PENDING", "La solicitud ya fue revisada");
        }
        return exception;
    }

    private AffectedResolution resolutionFor(AffectedResolution requested) {
        if (requested == null || access.actsAsOwnProfessional()) {
            return AffectedResolution.FLAG;
        }
        return requested;
    }

    private ExceptionReason resolveReason(ExceptionKind kind, ExceptionReason requested) {
        if (kind == ExceptionKind.EXTRA) {
            return requested == null || requested == ExceptionReason.EXTRA_HOURS
                    ? ExceptionReason.EXTRA_HOURS
                    : ExceptionReason.OTHER;
        }
        if (requested == ExceptionReason.EXTRA_HOURS) {
            throw new BadRequestException("Ese motivo es solo para horas extra");
        }
        return requested != null ? requested : ExceptionReason.OTHER;
    }

    private ExceptionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ExceptionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException invalid) {
            throw new BadRequestException("Estado inválido. Valores permitidos: PENDING, APPROVED, REJECTED, CANCELLED");
        }
    }

    private long pendingFor(Long exceptionId) {
        return counts(List.of(exceptionId)).getOrDefault(exceptionId, new long[2])[1];
    }

    private Map<Long, long[]> counts(List<Long> exceptionIds) {
        Map<Long, long[]> result = new HashMap<>();
        if (exceptionIds.isEmpty()) {
            return result;
        }
        for (Object[] row : appointmentRepository.countByExceptions(TenantContext.requireTenantId(),
                statuses.idOf(AppointmentStatusCode.SCHEDULED), exceptionIds)) {
            result.put((Long) row[0], new long[]{((Number) row[1]).longValue(),
                    row[2] != null ? ((Number) row[2]).longValue() : 0L});
        }
        return result;
    }

    private ScheduleExceptionResponse single(ScheduleException exception) {
        return toResponses(List.of(exception)).get(0);
    }

    private List<ScheduleExceptionResponse> toResponses(List<ScheduleException> exceptions) {
        if (exceptions.isEmpty()) {
            return List.of();
        }
        Map<Long, long[]> counts = counts(exceptions.stream().map(ScheduleException::getId).toList());
        Set<Long> userIds = exceptions.stream()
                .map(ScheduleException::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> names.put(user.getId(),
                user.getPerson() != null ? user.getPerson().fullName() : user.getEmail()));
        return exceptions.stream().map(exception -> {
            long[] count = counts.getOrDefault(exception.getId(), new long[2]);
            return new ScheduleExceptionResponse(
                    exception.getId(),
                    exception.getDoctor().getId(),
                    terminology.professionalName(exception.getDoctor().getPerson()),
                    exception.branchId(),
                    exception.getBranch() != null ? exception.getBranch().getName() : null,
                    exception.getKind().name(),
                    exception.getStartDate(),
                    exception.getEndDate(),
                    exception.getStartTime() != null ? exception.getStartTime().format(TIME) : null,
                    exception.getEndTime() != null ? exception.getEndTime().format(TIME) : null,
                    exception.isFullDay(),
                    exception.getSlotDurationMinutes(),
                    exception.getReason().name(),
                    exception.getReason().label(),
                    exception.getNote(),
                    exception.getStatus().name(),
                    exception.getCreatedAt(),
                    Optional.ofNullable(exception.getCreatedBy()).map(names::get).orElse(null),
                    exception.getReviewedAt(),
                    exception.getReviewNote(),
                    exception.getCancelledAt(),
                    count[0],
                    count[1]);
        }).toList();
    }

    private String describeDates(ScheduleException exception) {
        String dates = exception.getStartDate().equals(exception.getEndDate())
                ? DAY.format(exception.getStartDate())
                : DAY.format(exception.getStartDate()) + " al " + DAY.format(exception.getEndDate());
        return exception.isFullDay() ? dates : dates + ", " + exception.getStartTime().format(TIME) + "–"
                + exception.getEndTime().format(TIME);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
