package com.medical.medcore.appointment.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.domain.AppointmentFlowHistory;
import com.medical.medcore.appointment.domain.AppointmentFlowStatus;
import com.medical.medcore.appointment.domain.AppointmentReschedule;
import com.medical.medcore.appointment.domain.AppointmentStatusCode;
import com.medical.medcore.appointment.domain.BookingSource;
import com.medical.medcore.appointment.dto.AppointmentFlowEventResponse;
import com.medical.medcore.appointment.dto.AppointmentResponse;
import com.medical.medcore.appointment.dto.CancelAppointmentRequest;
import com.medical.medcore.appointment.dto.CreateAppointmentRequest;
import com.medical.medcore.appointment.dto.RescheduleAppointmentRequest;
import com.medical.medcore.appointment.repository.AppointmentFlowHistoryRepository;
import com.medical.medcore.appointment.repository.AppointmentRepository;
import com.medical.medcore.appointment.repository.AppointmentRescheduleRepository;
import com.medical.medcore.branch.domain.Branch;
import com.medical.medcore.branch.repository.BranchRepository;
import com.medical.medcore.catalog.domain.AppointmentType;
import com.medical.medcore.catalog.domain.TenantAppointmentType;
import com.medical.medcore.catalog.repository.AppointmentTypeRepository;
import com.medical.medcore.catalog.repository.TenantAppointmentTypeRepository;
import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.doctor.repository.DoctorBranchRepository;
import com.medical.medcore.doctor.repository.DoctorRepository;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.repository.PatientRepository;
import com.medical.medcore.platform.domain.ClinicProcess;
import com.medical.medcore.platform.service.TenantProcessConfigService;
import com.medical.medcore.shared.api.PageableResponse;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.BranchScope;
import com.medical.medcore.shared.security.SecurityUtils;
import com.medical.medcore.shared.tenancy.TenantContext;
import com.medical.medcore.triage.repository.TriageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final long NO_EXCLUSION = -1L;
    private static final String NOT_FOUND = "Cita no encontrada";
    private static final String CODE_SLOT_TAKEN = "SLOT_TAKEN";
    private static final String CODE_NOT_TODAY = "APPOINTMENT_NOT_TODAY";
    private static final String CODE_TRIAGE_REQUIRED = "TRIAGE_REQUIRED";
    private static final String CODE_NOT_ACTIVE = "APPOINTMENT_NOT_ACTIVE";
    private static final String SLOT_UNAVAILABLE = "El horario seleccionado ya no está disponible";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentRescheduleRepository rescheduleRepository;
    private final AppointmentFlowHistoryRepository flowHistoryRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final PatientRepository patientRepository;
    private final BranchRepository branchRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final TenantAppointmentTypeRepository tenantAppointmentTypeRepository;
    private final TriageRepository triageRepository;
    private final TenantProcessConfigService processConfigService;
    private final AvailabilityService availabilityService;
    private final BookingPolicy bookingPolicy;
    private final AppointmentStatuses statuses;
    private final AppointmentAccess access;
    private final AppointmentMapper mapper;

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        boolean bookedByStaff = SecurityUtils.isStaff();
        Patient patient = bookedByStaff ? requirePatient(request.patientId(), tenantId) : access.currentPatient();
        BookingSource source = bookedByStaff
                ? Optional.ofNullable(request.bookingSource()).orElse(BookingSource.RECEPTION)
                : BookingSource.SELF;
        return mapper.toResponse(book(tenantId, patient, request.doctorId(), request.branchId(),
                request.scheduledAt(), request.appointmentTypeId(), request.reason(), source, bookedByStaff));
    }

    @Transactional
    public Appointment book(Long tenantId, Patient patient, Long doctorId, Long branchId, LocalDateTime scheduledAt,
                            Long appointmentTypeId, String reason, BookingSource source, boolean bookedByStaff) {
        if (Boolean.FALSE.equals(patient.getIsActive())) {
            throw new BadRequestException("El paciente está inactivo");
        }
        Doctor doctor = doctorRepository.findByIdAndTenantId(doctorId, tenantId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado"));
        if (!Boolean.TRUE.equals(doctor.getIsActive())) {
            throw new BadRequestException("El doctor no está disponible");
        }
        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .filter(candidate -> !Boolean.FALSE.equals(candidate.getIsActive()))
                .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));
        if (!doctorBranchRepository.existsByDoctor_IdAndBranch_IdAndIsActiveTrue(doctorId, branchId)) {
            throw new BadRequestException("El doctor no atiende en la sucursal seleccionada");
        }
        BranchScope.assertAccess(branchId);

        AppointmentType type = resolveType(appointmentTypeId);
        Optional<TenantAppointmentType> tenantType = type == null
                ? Optional.empty()
                : tenantAppointmentTypeRepository.findByTenantIdAndAppointmentType_Id(tenantId, type.getId());
        int requestedMinutes = tenantType.map(TenantAppointmentType::getDurationMinutes)
                .filter(minutes -> minutes != null && minutes > 0)
                .orElse(type != null && type.getDurationMinutes() != null ? type.getDurationMinutes() : 0);

        int duration = availabilityService.resolveDuration(doctorId, branchId, scheduledAt, requestedMinutes);
        assertBookableTime(scheduledAt, duration, bookedByStaff);
        assertSlotFree(tenantId, doctorId, patient.getId(), scheduledAt, duration, NO_EXCLUSION);

        Appointment appointment = appointmentRepository.saveAndFlush(Appointment.builder()
                .tenantId(tenantId)
                .patient(patient)
                .doctor(doctor)
                .branch(branch)
                .scheduledAt(scheduledAt)
                .durationMinutes(duration)
                .statusId(statuses.idOf(AppointmentStatusCode.SCHEDULED))
                .appointmentTypeId(type != null ? type.getId() : null)
                .price(tenantType.map(TenantAppointmentType::getPrice).orElse(null))
                .reason(clean(reason))
                .bookingSource(source)
                .build());
        recordFlow(appointment, null, AppointmentFlowStatus.SCHEDULED.name(), null);
        return appointment;
    }

    public AppointmentResponse findById(Long id) {
        return mapper.toResponse(loadAccessible(id));
    }

    public List<AppointmentFlowEventResponse> history(Long id) {
        Appointment appointment = loadAccessible(id);
        return flowHistoryRepository.findByAppointmentIdOrderByIdAsc(appointment.getId()).stream()
                .map(event -> new AppointmentFlowEventResponse(event.getId(), event.getFromStatus(),
                        event.getFlowStatus(), event.getChangedAt(), event.getChangedBy(), event.getNote()))
                .toList();
    }

    public PageableResponse<AppointmentResponse> findAll(int page, int size, Long doctorId, Long patientId,
                                                         Long statusId, String status, LocalDate date,
                                                         String flowStatus) {
        Long tenantId = TenantContext.requireTenantId();
        Pageable pageable = pageRequest(page, size);
        BranchScope scope = BranchScope.resolve(null);
        if (scope.seesNothing()) {
            return PageableResponse.from(Page.empty(pageable));
        }
        return PageableResponse.from(search(tenantId, access.restrictDoctorFilter(doctorId), patientId,
                statuses.resolveFilter(statusId, status), date, flowStatus, scope, pageable));
    }

    public PageableResponse<AppointmentResponse> findMine(int page, int size, Long statusId, String status,
                                                          LocalDate date, String flowStatus) {
        Patient patient = access.currentPatient();
        return PageableResponse.from(search(patient.getTenantId(), null, patient.getId(),
                statuses.resolveFilter(statusId, status), date, flowStatus, BranchScope.resolve(null),
                pageRequest(page, size)));
    }

    public List<AppointmentResponse> getCalendar(LocalDate startDate, LocalDate endDate, Long doctorId, Long branchId) {
        Long tenantId = TenantContext.requireTenantId();
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("El rango de fechas es inválido");
        }
        BranchScope scope = BranchScope.resolve(branchId);
        if (scope.seesNothing()) {
            return List.of();
        }
        return mapper.toResponses(appointmentRepository.findForCalendar(tenantId, startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(), access.restrictDoctorFilter(doctorId),
                scope.restricted(), scope.queryIds()));
    }

    public List<AppointmentResponse> getQueue(Long branchId, Long doctorId, LocalDate date) {
        Long tenantId = TenantContext.requireTenantId();
        LocalDate target = date != null ? date : bookingPolicy.today();
        BranchScope scope = BranchScope.resolve(branchId);
        if (scope.seesNothing()) {
            return List.of();
        }
        return mapper.toResponses(appointmentRepository.findQueue(tenantId, target.atStartOfDay(),
                target.plusDays(1).atStartOfDay(), scope.restricted(), scope.queryIds(),
                access.restrictDoctorFilter(doctorId), statuses.inactiveIds()));
    }

    public List<AppointmentResponse> findPendingByDoctor(Long doctorId) {
        return mapper.toResponses(appointmentRepository.findPendingByDoctor(
                TenantContext.requireTenantId(), doctorId, statuses.inactiveIds(), bookingPolicy.now()).stream()
                .filter(appointment -> statuses.is(appointment.getStatusId(), AppointmentStatusCode.SCHEDULED))
                .toList());
    }

    @Transactional
    public AppointmentResponse reschedule(Long id, RescheduleAppointmentRequest request) {
        Appointment appointment = loadAccessible(id);
        assertScheduled(appointment);
        if (appointment.flow() != AppointmentFlowStatus.SCHEDULED) {
            throw new ConflictException(CODE_NOT_ACTIVE, "No se puede reprogramar una cita que ya inició su atención");
        }
        boolean staff = SecurityUtils.isStaff();
        LocalDateTime newStart = request.newScheduledAt();
        if (newStart.equals(appointment.getScheduledAt())) {
            throw new BadRequestException("La nueva fecha debe ser distinta de la actual");
        }
        int duration = availabilityService.resolveDuration(appointment.getDoctor().getId(),
                appointment.getBranch().getId(), newStart, appointment.getDurationMinutes());
        assertBookableTime(newStart, duration, staff);
        assertSlotFree(appointment.getTenantId(), appointment.getDoctor().getId(), appointment.getPatient().getId(),
                newStart, duration, appointment.getId());

        rescheduleRepository.save(AppointmentReschedule.builder()
                .appointment(appointment)
                .oldScheduledAt(appointment.getScheduledAt())
                .newScheduledAt(newStart)
                .reason(clean(request.reason()))
                .createdBy(TenantContext.getCurrentUserId())
                .build());
        appointment.setScheduledAt(newStart);
        appointment.setDurationMinutes(duration);
        return mapper.toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    @Transactional
    public AppointmentResponse cancel(Long id, CancelAppointmentRequest request) {
        Appointment appointment = loadAccessible(id);
        assertScheduled(appointment);
        AppointmentFlowStatus flow = appointment.flow();
        if (!flow.isBeforeConsultation()) {
            throw new ConflictException(CODE_NOT_ACTIVE, "No se puede cancelar una cita que ya está en atención");
        }
        if (!SecurityUtils.isStaff() && flow != AppointmentFlowStatus.SCHEDULED) {
            throw new ConflictException(CODE_NOT_ACTIVE, "La cita ya fue registrada en recepción. Solicita la cancelación en la clínica");
        }
        appointment.cancel(statuses.idOf(AppointmentStatusCode.CANCELLED), request.reason().trim(),
                TenantContext.getCurrentUserId(), bookingPolicy.now());
        appointmentRepository.save(appointment);
        recordFlow(appointment, flow.name(), AppointmentStatusCode.CANCELLED.name(), request.reason().trim());
        return mapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse markNoShow(Long id) {
        Appointment appointment = loadAccessible(id);
        assertScheduled(appointment);
        AppointmentFlowStatus flow = appointment.flow();
        if (flow != AppointmentFlowStatus.SCHEDULED) {
            throw new ConflictException(CODE_NOT_ACTIVE, "El paciente ya registró su llegada");
        }
        if (appointment.getScheduledAt().isAfter(bookingPolicy.now())) {
            throw new BadRequestException("Solo se puede marcar inasistencia después de la hora de la cita");
        }
        appointment.markNoShow(statuses.idOf(AppointmentStatusCode.NO_SHOW), bookingPolicy.now());
        appointmentRepository.save(appointment);
        recordFlow(appointment, flow.name(), AppointmentStatusCode.NO_SHOW.name(), null);
        return mapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse updateFlowStatus(Long id, String requestedFlow) {
        Appointment appointment = loadAccessible(id);
        assertScheduled(appointment);
        AppointmentFlowStatus current = appointment.flow();
        AppointmentFlowStatus target = parseFlow(requestedFlow);
        if (current == target) {
            throw new BadRequestException("La cita ya se encuentra en estado " + target.name());
        }
        if (!current.canTransitionTo(target)) {
            throw new BadRequestException("INVALID_TRANSITION",
                    "Transición de estado no permitida: " + current.name() + " -> " + target.name());
        }
        boolean startsVisit = target == AppointmentFlowStatus.WAITING || target == AppointmentFlowStatus.IN_PROCESS;
        if (startsVisit && !bookingPolicy.isToday(appointment.getScheduledAt())) {
            throw new ConflictException(CODE_NOT_TODAY,
                    "Solo se puede registrar la llegada o iniciar la atención el día de la cita ("
                            + appointment.getScheduledAt().toLocalDate() + ").");
        }
        if (target == AppointmentFlowStatus.IN_PROCESS && current.isBeforeConsultation() && triageMissing(appointment)) {
            throw new ConflictException(CODE_TRIAGE_REQUIRED,
                    "No se puede iniciar la consulta: el paciente aún no ha completado el triaje.");
        }
        applyFlow(appointment, target, null);
        return mapper.toResponse(appointment);
    }

    @Transactional
    public void applyFlow(Appointment appointment, AppointmentFlowStatus target, String note) {
        String previous = appointment.getFlowStatus();
        appointment.moveTo(target, bookingPolicy.now());
        if (target == AppointmentFlowStatus.COMPLETED) {
            appointment.setStatusId(statuses.idOf(AppointmentStatusCode.COMPLETED));
        }
        appointmentRepository.save(appointment);
        recordFlow(appointment, previous, target.name(), note);
    }

    public Appointment loadAccessible(Long id) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        access.assertCanAccess(appointment);
        return appointment;
    }

    public void assertScheduled(Appointment appointment) {
        if (!statuses.is(appointment.getStatusId(), AppointmentStatusCode.SCHEDULED)) {
            throw new ConflictException(CODE_NOT_ACTIVE,
                    "La cita no está activa (estado: " + statuses.codeOf(appointment.getStatusId()) + ")");
        }
    }

    private Page<AppointmentResponse> search(Long tenantId, Long doctorId, Long patientId, Long statusId,
                                             LocalDate date, String flowStatus, BranchScope scope, Pageable pageable) {
        LocalDateTime from = date != null ? date.atStartOfDay() : null;
        LocalDateTime to = date != null ? date.plusDays(1).atStartOfDay() : null;
        String flow = flowStatus == null || flowStatus.isBlank() ? null : parseFlow(flowStatus).name();
        Page<Appointment> result = appointmentRepository.findByFilters(tenantId, doctorId, patientId, statusId,
                from, to, flow, scope.restricted(), scope.queryIds(), pageable);
        return new PageImpl<>(mapper.toResponses(result.getContent()), pageable, result.getTotalElements());
    }

    private void assertBookableTime(LocalDateTime start, int duration, boolean bookedByStaff) {
        boolean bookable = bookedByStaff
                ? bookingPolicy.isBookableByStaff(start, duration)
                : bookingPolicy.isBookable(start);
        if (!bookable) {
            throw new BadRequestException(CODE_SLOT_TAKEN, SLOT_UNAVAILABLE);
        }
    }

    private void assertSlotFree(Long tenantId, Long doctorId, Long patientId, LocalDateTime start, int duration,
                                Long excludeId) {
        LocalDateTime end = start.plusMinutes(duration);
        List<Long> inactive = statuses.inactiveIds();
        if (appointmentRepository.countDoctorOverlaps(tenantId, doctorId, start, end, inactive, excludeId) > 0) {
            throw new ConflictException(CODE_SLOT_TAKEN, SLOT_UNAVAILABLE);
        }
        if (appointmentRepository.countPatientOverlaps(tenantId, patientId, start, end, inactive, excludeId) > 0) {
            throw new ConflictException("PATIENT_BUSY", "El paciente ya tiene una cita en ese horario");
        }
    }

    private boolean triageMissing(Appointment appointment) {
        boolean triageEnabled = Boolean.TRUE.equals(processConfigService.getConfig().get(ClinicProcess.TRIAGE));
        return triageEnabled && !triageRepository.existsByAppointmentId(appointment.getId());
    }

    private Patient requirePatient(Long patientId, Long tenantId) {
        if (patientId == null) {
            throw new BadRequestException("patientId es requerido");
        }
        return patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));
    }

    private AppointmentType resolveType(Long appointmentTypeId) {
        if (appointmentTypeId == null) {
            return null;
        }
        return appointmentTypeRepository.findById(appointmentTypeId)
                .filter(type -> !Boolean.FALSE.equals(type.getIsActive()))
                .orElseThrow(() -> new BadRequestException("Tipo de cita inválido"));
    }

    private AppointmentFlowStatus parseFlow(String raw) {
        try {
            return AppointmentFlowStatus.from(raw);
        } catch (IllegalArgumentException invalid) {
            throw new BadRequestException("flowStatus inválido. Valores permitidos: "
                    + "SCHEDULED, WAITING, CALLED, IN_PROCESS, PENDING_PAYMENT, COMPLETED");
        }
    }

    private void recordFlow(Appointment appointment, String from, String to, String note) {
        flowHistoryRepository.save(AppointmentFlowHistory.builder()
                .appointment(appointment)
                .fromStatus(from)
                .flowStatus(to)
                .changedAt(bookingPolicy.now())
                .changedBy(TenantContext.getCurrentUserId())
                .note(note != null && note.length() > 255 ? note.substring(0, 255) : note)
                .build());
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("scheduledAt").descending());
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
