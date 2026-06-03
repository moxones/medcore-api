package com.medical.medcore.service.appointment.impl;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.CancelAppointmentRequest;
import com.medical.medcore.dto.request.CreateAppointmentRequest;
import com.medical.medcore.dto.request.RescheduleAppointmentRequest;
import com.medical.medcore.dto.request.UpdateAppointmentFlowRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.dto.response.AvailabilitySlotResponse;
import com.medical.medcore.dto.response.DayAvailabilityResponse;
import com.medical.medcore.dto.response.SpecialtySummaryResponse;
import com.medical.medcore.dto.response.TimeSlotResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.AppointmentReschedule;
import com.medical.medcore.entity.Branch;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.DoctorBranch;
import com.medical.medcore.entity.DoctorSchedule;
import com.medical.medcore.entity.DoctorSpecialty;
import com.medical.medcore.entity.Patient;
import com.medical.medcore.entity.enums.AppointmentFlowStatus;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.Specialty;
import com.medical.medcore.entity.User;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.security.authorization.SecurityUtils;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.AppointmentRescheduleRepository;
import com.medical.medcore.repository.AppointmentTypeRepository;
import com.medical.medcore.repository.BranchRepository;
import com.medical.medcore.repository.DoctorBranchRepository;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.repository.DoctorScheduleRepository;
import com.medical.medcore.repository.DoctorSpecialtyRepository;
import com.medical.medcore.repository.PatientRepository;
import com.medical.medcore.service.appointment.AppointmentService;
import com.medical.medcore.types.PageableResponse;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentRescheduleRepository rescheduleRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final PatientRepository patientRepository;
    private final BranchRepository branchRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final UserRepository userRepository;

    private static final Long STATUS_SCHEDULED = 1L;
    private static final Long STATUS_CANCELLED = 4L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Placeholder no-existente: cuando no se aplica el filtro por sucursal, la cláusula
    // IN nunca se evalúa pero JPQL exige una lista no vacía válida.
    private static final List<Long> NO_BRANCH_PLACEHOLDER = List.of(-1L);

    @Override
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        Long effectivePatientId = resolveBookingPatientId(request.patientId(), tenantId);

        // 1. Validar que el paciente existe y pertenece al tenant
        Patient patient = patientRepository.findByIdAndTenantId(effectivePatientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));

        // 2. Validar que el doctor existe, está activo y pertenece al tenant
        Doctor doctor = doctorRepository.findByIdAndTenantId(request.doctorId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado"));
        if (Boolean.FALSE.equals(doctor.getIsActive())) {
            throw new BadRequestException("El doctor no está disponible");
        }

        // 3. Validar que el doctor trabaja en la sucursal indicada
        boolean doctorInBranch = doctorBranchRepository
                .existsByDoctor_IdAndBranch_IdAndIsActiveTrue(request.doctorId(), request.branchId());
        if (!doctorInBranch) {
            throw new BadRequestException("El doctor no atiende en la sucursal seleccionada");
        }

        // 3.1 El personal operativo solo puede registrar citas en sus sucursales asignadas
        if (SecurityUtils.isBranchScoped()) {
            List<Long> allowed = TenantContext.getBranchIds();
            if (allowed == null || !allowed.contains(request.branchId())) {
                throw new AccessDeniedException("No puedes crear citas en esta sucursal");
            }
        }

        // 4. Validar que el slot no esté ocupado (anti-overbooking)
        boolean slotTaken = appointmentRepository.existsByTenantIdAndDoctorIdAndScheduledAtAndStatusIdNot(
                tenantId, request.doctorId(), request.scheduledAt(), STATUS_CANCELLED);
        if (slotTaken) {
            throw new BadRequestException("El horario seleccionado ya no está disponible");
        }

        Branch branch = new Branch();
        branch.setId(request.branchId());

        Appointment appointment = Appointment.builder()
                .tenantId(tenantId)
                .patient(patient)
                .doctor(doctor)
                .branch(branch)
                .scheduledAt(request.scheduledAt())
                .reason(request.reason())
                .statusId(STATUS_SCHEDULED)
                .appointmentTypeId(request.appointmentTypeId())
                .durationMinutes(30)
                .flowStatus(AppointmentFlowStatus.SCHEDULED.name())
                .bookingSource(request.bookingSource())
                .build();

        appointment = appointmentRepository.save(appointment);

        return appointmentRepository.findByIdWithDetails(appointment.getId(), tenantId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Appointment appointment = appointmentRepository.findByIdWithDetails(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        assertCanAccessAppointment(appointment, tenantId);
        assertStaffBranchAccess(appointment);
        return mapToResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<AppointmentResponse> findAll(int page, int size, Long doctorId, Long patientId, Long statusId, LocalDate date, String flowStatus) {
        Long tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledAt").descending());

        LocalDateTime startDate = date != null ? date.atStartOfDay() : null;
        LocalDateTime endDate = date != null ? date.plusDays(1).atStartOfDay() : null;

        BranchScope scope = branchScope(null);
        if (scope.empty()) {
            return PageableResponse.from(Page.<Appointment>empty(pageable).map(this::mapToResponse));
        }

        Page<Appointment> resultPage = appointmentRepository.findByFilters(
                tenantId, doctorId, patientId, statusId, startDate, endDate, flowStatus,
                scope.apply(), scope.ids(), pageable);

        return PageableResponse.from(resultPage.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getCalendar(LocalDate startDate, LocalDate endDate, Long doctorId, Long branchId) {
        Long tenantId = TenantContext.requireTenantId();

        BranchScope scope = branchScope(branchId);
        if (scope.empty()) {
            return List.of();
        }

        return appointmentRepository.findForCalendar(
                        tenantId,
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay(),
                        doctorId,
                        scope.apply(),
                        scope.ids())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getQueue(Long branchId, Long doctorId, LocalDate date) {
        Long tenantId = TenantContext.requireTenantId();
        LocalDate target = date != null ? date : LocalDate.now();

        BranchScope scope = branchScope(branchId);
        if (scope.empty()) {
            return List.of();
        }

        return appointmentRepository.findQueue(
                        tenantId,
                        target.atStartOfDay(),
                        target.plusDays(1).atStartOfDay(),
                        scope.apply(),
                        scope.ids(),
                        doctorId,
                        STATUS_CANCELLED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(Long doctorId, Long branchId, LocalDate date) {
        Long tenantId = TenantContext.requireTenantId();

        // dayOfWeek: Java MONDAY=1 → nuestro esquema 0=Lunes..6=Domingo
        int dayOfWeek = date.getDayOfWeek().getValue() - 1;

        List<DoctorSchedule> schedules;
        if (branchId != null) {
            schedules = doctorScheduleRepository.findActiveByDoctorIdAndBranchIdAndDay(doctorId, branchId, dayOfWeek);
        } else {
            schedules = doctorScheduleRepository.findByDoctorIdWithFilters(doctorId, dayOfWeek, true);
        }

        schedules = schedules.stream()
                .filter(s -> (s.getValidFrom() == null || !date.isBefore(s.getValidFrom())) &&
                             (s.getValidUntil() == null || !date.isAfter(s.getValidUntil())))
                .toList();

        if (schedules.isEmpty()) {
            return List.of();
        }

        List<Appointment> existing = appointmentRepository.findByDoctorAndDate(
                tenantId, doctorId,
                date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                STATUS_CANCELLED);

        Set<LocalTime> occupiedTimes = existing.stream()
                .map(a -> a.getScheduledAt().toLocalTime())
                .collect(Collectors.toSet());

        List<TimeSlotResponse> slots = new ArrayList<>();
        for (DoctorSchedule schedule : schedules) {
            int slotMinutes = schedule.getSlotDurationMinutes() != null ? schedule.getSlotDurationMinutes() : 30;
            LocalTime current = schedule.getStartTime();
            LocalTime end = schedule.getEndTime();

            while (!current.plusMinutes(slotMinutes).isAfter(end)) {
                slots.add(new TimeSlotResponse(current, current.plusMinutes(slotMinutes), !occupiedTimes.contains(current)));
                current = current.plusMinutes(slotMinutes);
            }
        }

        return slots;
    }

    @Override
    @Transactional
    public void reschedule(Long id, RescheduleAppointmentRequest request) {
        Appointment appointment = getOwnedAppointment(id);
        Long tenantId = appointment.getTenantId();
        assertCanAccessAppointment(appointment, tenantId);

        // Validar que el nuevo slot no esté ocupado
        boolean slotTaken = appointmentRepository.existsByTenantIdAndDoctorIdAndScheduledAtAndStatusIdNot(
                tenantId, appointment.getDoctor().getId(), request.newScheduledAt(), STATUS_CANCELLED);
        if (slotTaken) {
            throw new BadRequestException("El horario seleccionado ya no está disponible");
        }

        // Registrar historial de reprogramación
        rescheduleRepository.save(AppointmentReschedule.builder()
                .appointment(appointment)
                .oldScheduledAt(appointment.getScheduledAt())
                .newScheduledAt(request.newScheduledAt())
                .reason(request.reason())
                .build());

        appointment.setScheduledAt(request.newScheduledAt());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse updateFlowStatus(Long id, UpdateAppointmentFlowRequest request) {
        Appointment appointment = getOwnedAppointment(id);

        AppointmentFlowStatus current = AppointmentFlowStatus.from(appointment.getFlowStatus());

        AppointmentFlowStatus target;
        try {
            target = AppointmentFlowStatus.from(request.flowStatus());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("flowStatus inválido. Valores permitidos: " +
                    "SCHEDULED, WAITING, CALLED, IN_PROCESS, PENDING_PAYMENT, COMPLETED");
        }

        if (current == target) {
            throw new BadRequestException("La cita ya se encuentra en estado " + target.name());
        }
        if (!current.canTransitionTo(target)) {
            throw new BadRequestException(
                    "Transición de estado no permitida: " + current.name() + " -> " + target.name());
        }

        // Sellar el timestamp de la transición (solo la primera vez que se entra al estado,
        // para no falsear los timers si recepción retrocede y vuelve a avanzar).
        LocalDateTime now = LocalDateTime.now();
        switch (target) {
            case WAITING -> {
                if (appointment.getCheckedInAt() == null) appointment.setCheckedInAt(now);
            }
            case CALLED -> {
                if (appointment.getCalledAt() == null) appointment.setCalledAt(now);
            }
            case IN_PROCESS -> {
                if (appointment.getStartedAt() == null) appointment.setStartedAt(now);
            }
            case PENDING_PAYMENT -> {
                if (appointment.getFinishedAt() == null) appointment.setFinishedAt(now);
            }
            case COMPLETED -> {
                // Atajo IN_PROCESS -> COMPLETED: la consulta también terminó aquí.
                if (appointment.getFinishedAt() == null) appointment.setFinishedAt(now);
                if (appointment.getCompletedAt() == null) appointment.setCompletedAt(now);
            }
            default -> { /* SCHEDULED no es un destino alcanzable por flow-status */ }
        }

        appointment.setFlowStatus(target.name());
        appointmentRepository.save(appointment);

        Long tenantId = appointment.getTenantId();
        return appointmentRepository.findByIdWithDetails(appointment.getId(), tenantId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
    }

    @Override
    @Transactional
    public void cancel(Long id, CancelAppointmentRequest request) {
        Appointment appointment = getOwnedAppointment(id);
        assertCanAccessAppointment(appointment, appointment.getTenantId());
        appointment.setStatusId(STATUS_CANCELLED);
        appointment.setReason(request.reason());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DayAvailabilityResponse> getAvailability(
            Long branchId, LocalDate fromDate, LocalDate toDate,
            Long specialtyId, Long doctorId, Long appointmentTypeId) {

        Long tenantId = TenantContext.requireTenantId();

        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new NotFoundException("Sede no encontrada"));

        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("El rango de fechas es inválido");
        }

        // 1. Doctors active in the branch
        List<DoctorBranch> doctorBranches = doctorBranchRepository.findActiveDoctorsByBranchId(branchId)
                .stream()
                .filter(db -> Boolean.TRUE.equals(db.getDoctor().getIsActive()))
                .collect(Collectors.toList());

        if (doctorId != null) {
            doctorBranches = doctorBranches.stream()
                    .filter(db -> db.getDoctor().getId().equals(doctorId))
                    .collect(Collectors.toList());
        }

        if (doctorBranches.isEmpty()) return List.of();

        List<Long> doctorIds = doctorBranches.stream()
                .map(db -> db.getDoctor().getId())
                .collect(Collectors.toList());

        // 2. Load specialties and optionally filter by specialtyId
        List<DoctorSpecialty> allSpecialties = doctorSpecialtyRepository.findByDoctorIdIn(doctorIds);
        Map<Long, String> doctorSpecialtyName = new HashMap<>();
        for (DoctorSpecialty ds : allSpecialties) {
            if (specialtyId == null || ds.getSpecialty().getId().equals(specialtyId)) {
                doctorSpecialtyName.putIfAbsent(ds.getDoctor().getId(), ds.getSpecialty().getName());
            }
        }

        if (specialtyId != null) {
            doctorIds = doctorIds.stream().filter(doctorSpecialtyName::containsKey).collect(Collectors.toList());
            if (doctorIds.isEmpty()) return List.of();
        }

        final List<Long> finalDoctorIds = doctorIds;
        Map<Long, Doctor> doctorMap = doctorBranches.stream()
                .filter(db -> finalDoctorIds.contains(db.getDoctor().getId()))
                .collect(Collectors.toMap(db -> db.getDoctor().getId(), DoctorBranch::getDoctor, (a, b) -> a));

        // 3. Slot duration override
        final int[] overrideDuration = {0};
        if (appointmentTypeId != null) {
            appointmentTypeRepository.findById(appointmentTypeId)
                    .ifPresent(at -> overrideDuration[0] = at.getDurationMinutes());
        }

        // 4. Batch-load schedules and appointments
        List<DoctorSchedule> allSchedules = doctorScheduleRepository
                .findActiveByDoctorIdInAndBranchId(finalDoctorIds, branchId);

        Map<Long, Map<Integer, List<DoctorSchedule>>> schedByDoctorDay = new HashMap<>();
        for (DoctorSchedule s : allSchedules) {
            schedByDoctorDay
                    .computeIfAbsent(s.getDoctor().getId(), k -> new HashMap<>())
                    .computeIfAbsent(s.getDayOfWeek(), k -> new ArrayList<>())
                    .add(s);
        }

        List<Appointment> existing = appointmentRepository.findByDoctorsAndDateRange(
                tenantId, finalDoctorIds,
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay(), STATUS_CANCELLED);

        Map<Long, Map<LocalDate, Set<LocalTime>>> occupied = new HashMap<>();
        for (Appointment a : existing) {
            occupied
                    .computeIfAbsent(a.getDoctor().getId(), k -> new HashMap<>())
                    .computeIfAbsent(a.getScheduledAt().toLocalDate(), k -> new HashSet<>())
                    .add(a.getScheduledAt().toLocalTime());
        }

        // 5. Generate slots day by day
        List<DayAvailabilityResponse> result = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            int dow = date.getDayOfWeek().getValue() - 1; // 0=Mon..6=Sun
            List<AvailabilitySlotResponse> daySlots = new ArrayList<>();

            for (Long did : finalDoctorIds) {
                List<DoctorSchedule> schedules = schedByDoctorDay
                        .getOrDefault(did, Map.of())
                        .getOrDefault(dow, List.of());

                for (DoctorSchedule sched : schedules) {
                    if (sched.getValidFrom() != null && date.isBefore(sched.getValidFrom())) continue;
                    if (sched.getValidUntil() != null && date.isAfter(sched.getValidUntil())) continue;

                    int slotMin = overrideDuration[0] > 0 ? overrideDuration[0]
                            : (sched.getSlotDurationMinutes() != null ? sched.getSlotDurationMinutes() : 30);

                    Set<LocalTime> taken = occupied
                            .getOrDefault(did, Map.of())
                            .getOrDefault(date, Set.of());

                    LocalTime cur = sched.getStartTime();
                    LocalTime end = sched.getEndTime();
                    Doctor doctor = doctorMap.get(did);
                    Person person = doctor.getPerson();
                    String name = buildDoctorName(person);
                    String initials = buildInitials(person);
                    String specName = doctorSpecialtyName.getOrDefault(did, "");

                    while (!cur.plusMinutes(slotMin).isAfter(end)) {
                        if (!taken.contains(cur)) {
                            daySlots.add(new AvailabilitySlotResponse(
                                    cur.format(TIME_FMT),
                                    cur.plusMinutes(slotMin).format(TIME_FMT),
                                    did, name, initials, specName, slotMin));
                        }
                        cur = cur.plusMinutes(slotMin);
                    }
                }
            }

            if (!daySlots.isEmpty()) {
                daySlots.sort(Comparator.comparing(AvailabilitySlotResponse::startTime));
                result.add(new DayAvailabilityResponse(date.toString(), daySlots));
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecialtySummaryResponse> getSpecialtiesSummary(Long branchId) {
        Long tenantId = TenantContext.requireTenantId();

        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new NotFoundException("Sede no encontrada"));

        List<Long> doctorIds = doctorBranchRepository.findActiveDoctorsByBranchId(branchId)
                .stream()
                .filter(db -> Boolean.TRUE.equals(db.getDoctor().getIsActive()))
                .map(db -> db.getDoctor().getId())
                .collect(Collectors.toList());

        if (doctorIds.isEmpty()) return List.of();

        // Group doctors by specialty
        Map<Long, Specialty> specialtyById = new HashMap<>();
        Map<Long, Set<Long>> doctorsBySpecialty = new HashMap<>();
        for (DoctorSpecialty ds : doctorSpecialtyRepository.findByDoctorIdIn(doctorIds)) {
            Long specId = ds.getSpecialty().getId();
            specialtyById.put(specId, ds.getSpecialty());
            doctorsBySpecialty.computeIfAbsent(specId, k -> new HashSet<>()).add(ds.getDoctor().getId());
        }

        if (specialtyById.isEmpty()) return List.of();

        // Batch-load next 30 days data
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(30);

        List<DoctorSchedule> allSchedules = doctorScheduleRepository
                .findActiveByDoctorIdInAndBranchId(doctorIds, branchId);

        Map<Long, Map<Integer, List<DoctorSchedule>>> schedByDoctorDay = new HashMap<>();
        for (DoctorSchedule s : allSchedules) {
            schedByDoctorDay
                    .computeIfAbsent(s.getDoctor().getId(), k -> new HashMap<>())
                    .computeIfAbsent(s.getDayOfWeek(), k -> new ArrayList<>())
                    .add(s);
        }

        List<Appointment> existing = appointmentRepository.findByDoctorsAndDateRange(
                tenantId, doctorIds,
                today.atStartOfDay(), horizon.plusDays(1).atStartOfDay(), STATUS_CANCELLED);

        Map<Long, Map<LocalDate, Set<LocalTime>>> occupied = new HashMap<>();
        for (Appointment a : existing) {
            occupied
                    .computeIfAbsent(a.getDoctor().getId(), k -> new HashMap<>())
                    .computeIfAbsent(a.getScheduledAt().toLocalDate(), k -> new HashSet<>())
                    .add(a.getScheduledAt().toLocalTime());
        }

        List<SpecialtySummaryResponse> result = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> entry : doctorsBySpecialty.entrySet()) {
            Long specId = entry.getKey();
            Set<Long> docs = entry.getValue();
            Specialty specialty = specialtyById.get(specId);

            String nextDate = null;
            outer:
            for (LocalDate date = today; !date.isAfter(horizon); date = date.plusDays(1)) {
                int dow = date.getDayOfWeek().getValue() - 1;
                for (Long did : docs) {
                    List<DoctorSchedule> schedules = schedByDoctorDay
                            .getOrDefault(did, Map.of())
                            .getOrDefault(dow, List.of());
                    for (DoctorSchedule sched : schedules) {
                        if (sched.getValidFrom() != null && date.isBefore(sched.getValidFrom())) continue;
                        if (sched.getValidUntil() != null && date.isAfter(sched.getValidUntil())) continue;
                        int slotMin = sched.getSlotDurationMinutes() != null ? sched.getSlotDurationMinutes() : 30;
                        Set<LocalTime> taken = occupied.getOrDefault(did, Map.of()).getOrDefault(date, Set.of());
                        LocalTime cur = sched.getStartTime();
                        while (!cur.plusMinutes(slotMin).isAfter(sched.getEndTime())) {
                            if (!taken.contains(cur)) {
                                nextDate = date.toString();
                                break outer;
                            }
                            cur = cur.plusMinutes(slotMin);
                        }
                    }
                }
            }

            result.add(new SpecialtySummaryResponse(specId, specialty.getCode(), specialty.getName(),
                    docs.size(), nextDate));
        }

        result.sort(Comparator.comparing(SpecialtySummaryResponse::nextAvailableDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return result;
    }

    private String buildDoctorName(Person person) {
        String prefix = "F".equalsIgnoreCase(person.getGender()) ? "Dra." : "Dr.";
        return prefix + " " + person.getFirstName() + " " + person.getLastName();
    }

    private String buildInitials(Person person) {
        String f = (person.getFirstName() != null && !person.getFirstName().isEmpty())
                ? String.valueOf(person.getFirstName().charAt(0)).toUpperCase() : "";
        String l = (person.getLastName() != null && !person.getLastName().isEmpty())
                ? String.valueOf(person.getLastName().charAt(0)).toUpperCase() : "";
        return f + l;
    }

    private Appointment getOwnedAppointment(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        if (!appointment.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }
        assertStaffBranchAccess(appointment);
        return appointment;
    }

    /**
     * Restringe al personal operativo (recepción / asistente) a operar únicamente sobre citas
     * de las sucursales que tiene asignadas. Admins, super-admins y doctores no se ven afectados;
     * los pacientes se validan por separado en {@link #assertCanAccessAppointment}.
     */
    private void assertStaffBranchAccess(Appointment appointment) {
        if (!SecurityUtils.isBranchScoped()) {
            return;
        }
        List<Long> allowed = TenantContext.getBranchIds();
        Long branchId = appointment.getBranch() != null ? appointment.getBranch().getId() : null;
        if (branchId == null || allowed == null || !allowed.contains(branchId)) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }
    }

    /** null = sin restricción; lista vacía = no ve nada; lista con ids = restringido a esas sucursales. */
    private List<Long> resolveBranchScope(Long requestedBranchId) {
        if (!SecurityUtils.isBranchScoped()) {
            return requestedBranchId != null ? List.of(requestedBranchId) : null;
        }
        List<Long> allowed = TenantContext.getBranchIds();
        if (requestedBranchId != null) {
            if (allowed == null || !allowed.contains(requestedBranchId)) {
                throw new AccessDeniedException("No tienes acceso a esta sucursal");
            }
            return List.of(requestedBranchId);
        }
        return allowed != null ? allowed : List.of();
    }

    private BranchScope branchScope(Long requestedBranchId) {
        List<Long> scope = resolveBranchScope(requestedBranchId);
        if (scope == null) {
            return new BranchScope(false, NO_BRANCH_PLACEHOLDER, false);
        }
        if (scope.isEmpty()) {
            return new BranchScope(true, NO_BRANCH_PLACEHOLDER, true);
        }
        return new BranchScope(true, scope, false);
    }

    /** apply=true activa el filtro IN; empty=true significa "no debe ver nada" (cortocircuito). */
    private record BranchScope(boolean apply, List<Long> ids, boolean empty) {}

    private Long resolveBookingPatientId(Long requestedPatientId, Long tenantId) {
        if (SecurityUtils.isStaff()) {
            return requestedPatientId;
        }
        return resolveCurrentPatient(tenantId).getId();
    }

    private void assertCanAccessAppointment(Appointment appointment, Long tenantId) {
        if (SecurityUtils.isStaff()) {
            return;
        }
        Patient self = resolveCurrentPatient(tenantId);
        Patient apptPatient = appointment.getPatient();
        if (apptPatient == null || !apptPatient.getId().equals(self.getId())) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }
    }

    private Patient resolveCurrentPatient(Long tenantId) {
        Long userId = TenantContext.requireCurrentUserId();
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return patientRepository.findByPersonIdAndTenantId(user.getPerson().getId(), tenantId)
                .orElseThrow(() -> new AccessDeniedException("No tienes un paciente asociado para esta operación"));
    }

    private AppointmentResponse mapToResponse(Appointment a) {
        Patient patient = a.getPatient();
        Person patientPerson = patient != null ? patient.getPerson() : null;

        Doctor doctor = a.getDoctor();
        Person doctorPerson = doctor != null ? doctor.getPerson() : null;

        Branch branch = a.getBranch();

        return new AppointmentResponse(
                a.getId(),
                patient != null ? patient.getId() : null,
                patientPerson != null ? patientPerson.getFirstName() + " " + patientPerson.getLastName() : null,
                patientPerson != null ? patientPerson.getPhone() : null,
                doctor != null ? doctor.getId() : null,
                doctorPerson != null ? doctorPerson.getFirstName() + " " + doctorPerson.getLastName() : null,
                branch != null ? branch.getId() : null,
                branch != null ? branch.getName() : null,
                a.getScheduledAt(),
                a.getStatusId(),
                mapStatusIdToString(a.getStatusId()),
                a.getAppointmentTypeId(),
                a.getReason(),
                a.getDurationMinutes(),
                a.getFlowStatus(),
                a.getCreatedAt(),
                a.getBookingSource() != null ? a.getBookingSource().name() : null,
                a.getCheckedInAt(),
                a.getCalledAt(),
                a.getStartedAt(),
                a.getFinishedAt(),
                a.getCompletedAt(),
                null // amount: sin fuente de precio por tipo de cita todavía
        );
    }

    private String mapStatusIdToString(Long statusId) {
        if (statusId == null) return null;
        if (statusId.equals(1L)) return "scheduled";
        if (statusId.equals(3L)) return "completed";
        if (statusId.equals(4L)) return "cancelled";
        return "unknown";
    }
}
