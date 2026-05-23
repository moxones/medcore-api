package com.medical.medcore.service.appointment.impl;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.CancelAppointmentRequest;
import com.medical.medcore.dto.request.CreateAppointmentRequest;
import com.medical.medcore.dto.request.RescheduleAppointmentRequest;
import com.medical.medcore.dto.request.UpdateAppointmentFlowRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.dto.response.TimeSlotResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.AppointmentReschedule;
import com.medical.medcore.entity.Branch;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.DoctorSchedule;
import com.medical.medcore.entity.Patient;
import com.medical.medcore.entity.Person;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.AppointmentRescheduleRepository;
import com.medical.medcore.repository.DoctorBranchRepository;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.repository.DoctorScheduleRepository;
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
import java.util.ArrayList;
import java.util.List;
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
    private final PatientRepository patientRepository;

    private static final Long STATUS_SCHEDULED = 1L;
    private static final Long STATUS_CANCELLED = 4L;

    @Override
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        // 1. Validar que el paciente existe y pertenece al tenant
        Patient patient = patientRepository.findByIdAndTenantId(request.patientId(), tenantId)
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
                .flowStatus("WAITING")
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
        return appointmentRepository.findByIdWithDetails(id, tenantId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<AppointmentResponse> findAll(int page, int size, Long doctorId, Long patientId, Long statusId, LocalDate date, String flowStatus) {
        Long tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledAt").descending());

        LocalDateTime startDate = date != null ? date.atStartOfDay() : null;
        LocalDateTime endDate = date != null ? date.plusDays(1).atStartOfDay() : null;

        Page<Appointment> resultPage = appointmentRepository.findByFilters(
                tenantId, doctorId, patientId, statusId, startDate, endDate, flowStatus, pageable);

        return PageableResponse.from(resultPage.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getCalendar(LocalDate startDate, LocalDate endDate, Long doctorId, Long branchId) {
        Long tenantId = TenantContext.requireTenantId();

        return appointmentRepository.findForCalendar(
                        tenantId,
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay(),
                        doctorId,
                        branchId)
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
    public void updateFlowStatus(Long id, UpdateAppointmentFlowRequest request) {
        Appointment appointment = getOwnedAppointment(id);
        appointment.setFlowStatus(request.flowStatus());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void cancel(Long id, CancelAppointmentRequest request) {
        Appointment appointment = getOwnedAppointment(id);
        appointment.setStatusId(STATUS_CANCELLED);
        appointment.setReason(request.reason());
        appointmentRepository.save(appointment);
    }

    private Appointment getOwnedAppointment(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        if (!appointment.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }
        return appointment;
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
                a.getAppointmentTypeId(),
                a.getReason(),
                a.getDurationMinutes(),
                a.getFlowStatus(),
                a.getCreatedAt(),
                a.getBookingSource() != null ? a.getBookingSource().name() : null
        );
    }
}
