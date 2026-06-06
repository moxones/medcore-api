package com.medical.medcore.service.dashboard;

import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.response.DashboardSummaryResponse;
import com.medical.medcore.dto.response.DoctorAgendaItemResponse;
import com.medical.medcore.dto.response.DoctorDashboardSummaryResponse;
import com.medical.medcore.dto.response.DoctorRecentPatientResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.AppointmentType;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.Patient;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.User;
import com.medical.medcore.entity.enums.AppointmentFlowStatus;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.AppointmentTypeRepository;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.repository.MedicalEntryRepository;
import com.medical.medcore.repository.PaymentRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final MedicalEntryRepository medicalEntryRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;

    private static final Long STATUS_CANCELLED = 4L;
    private static final Long STATUS_COMPLETED = 3L;
    private static final int RECENT_PATIENTS_LIMIT = 5;

    public DashboardSummaryResponse getSummaryMetrics() {
        Long tenantId = TenantContext.requireTenantId();
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();

        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDateTime startOfMonthTime = startOfMonth.atStartOfDay();

        long totalToday = appointmentRepository.countByTenantAndDateRange(tenantId, startOfToday, endOfToday);
        long cancelledToday = appointmentRepository.countByTenantAndStatusAndDateRange(tenantId, STATUS_CANCELLED, startOfToday, endOfToday);
        long completedToday = appointmentRepository.countByTenantAndStatusAndDateRange(tenantId, STATUS_COMPLETED, startOfToday, endOfToday);

        BigDecimal revenueThisMonth = paymentRepository.sumCompletedPaymentsByTenantAndDateRange(tenantId, startOfMonthTime, endOfToday);
        if (revenueThisMonth == null) {
            revenueThisMonth = BigDecimal.ZERO;
        }

        double noShowRate = 0.0;
        if (totalToday > 0) {
            noShowRate = BigDecimal.valueOf((double) cancelledToday / totalToday * 100)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new DashboardSummaryResponse(
                totalToday,
                cancelledToday,
                completedToday,
                revenueThisMonth,
                noShowRate
        );
    }

    public java.util.List<com.medical.medcore.dto.response.DoctorProductivityResponse> getDoctorProductivity() {
        Long tenantId = TenantContext.requireTenantId();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        return appointmentRepository.getProductivityByDoctor(tenantId, startOfMonth, endOfMonth);
    }

    /**
     * Resumen del dashboard del médico autenticado: KPIs del día, próximo paciente,
     * agenda de hoy y pacientes atendidos recientemente. Resuelve al médico desde el token.
     */
    @Transactional(readOnly = true)
    public DoctorDashboardSummaryResponse getDoctorSummary() {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();

        Doctor doctor = resolveCurrentDoctor(tenantId, userId);
        Long doctorId = doctor.getId();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();

        // Agenda de hoy (citas no canceladas), ordenada por hora ascendente.
        List<Appointment> todays = appointmentRepository.findDoctorAgendaWithDetails(
                tenantId, doctorId, startOfToday, endOfToday, STATUS_CANCELLED);

        // Nombre legible del tipo de cita (id -> name).
        Map<Long, String> typeNames = appointmentTypeRepository.findAll().stream()
                .collect(Collectors.toMap(AppointmentType::getId, AppointmentType::getName, (a, b) -> a));

        // Pacientes ya vistos por este médico antes de hoy → para marcar isNewPatient.
        Set<Long> returningPatientIds = new HashSet<>(
                appointmentRepository.findPatientIdsSeenByDoctorBefore(
                        tenantId, doctorId, startOfToday, STATUS_CANCELLED));

        List<DoctorAgendaItemResponse> agenda = todays.stream()
                .map(a -> toAgendaItem(a, typeNames, returningPatientIds))
                .collect(Collectors.toList());

        // KPIs por flowStatus.
        long upcoming = countByFlow(agenda, AppointmentFlowStatus.SCHEDULED);
        long waiting = agenda.stream()
                .filter(i -> AppointmentFlowStatus.WAITING.name().equals(i.flowStatus())
                        || AppointmentFlowStatus.CALLED.name().equals(i.flowStatus()))
                .count();
        long inProgress = countByFlow(agenda, AppointmentFlowStatus.IN_PROCESS);
        long completedToday = countByFlow(agenda, AppointmentFlowStatus.COMPLETED);

        // Próximo paciente: la cita activa (no completada) más cercana en el tiempo.
        DoctorAgendaItemResponse nextPatient = agenda.stream()
                .filter(i -> !AppointmentFlowStatus.COMPLETED.name().equals(i.flowStatus()))
                .findFirst()
                .orElse(null);

        long pendingNotes = medicalEntryRepository.countUnsignedByCreator(tenantId, userId);
        long avgConsultationMinutes = averageConsultationMinutes(todays);

        List<DoctorRecentPatientResponse> recentPatients = buildRecentPatients(
                tenantId, doctorId, startOfToday, typeNames);

        return new DoctorDashboardSummaryResponse(
                agenda.size(),
                upcoming,
                waiting,
                inProgress,
                completedToday,
                pendingNotes,
                avgConsultationMinutes,
                nextPatient,
                agenda,
                recentPatients
        );
    }

    private Doctor resolveCurrentDoctor(Long tenantId, Long userId) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (user.getPerson() == null) {
            throw new AccessDeniedException("El usuario no tiene un médico asociado");
        }
        return doctorRepository.findByPersonIdAndTenantId(user.getPerson().getId(), tenantId)
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene un médico asociado"));
    }

    private long countByFlow(List<DoctorAgendaItemResponse> agenda, AppointmentFlowStatus status) {
        return agenda.stream().filter(i -> status.name().equals(i.flowStatus())).count();
    }

    private DoctorAgendaItemResponse toAgendaItem(Appointment a, Map<Long, String> typeNames,
                                                  Set<Long> returningPatientIds) {
        Patient patient = a.getPatient();
        Person person = patient != null ? patient.getPerson() : null;
        Long patientId = patient != null ? patient.getId() : null;
        String flowStatus = AppointmentFlowStatus.from(a.getFlowStatus()).name();

        return new DoctorAgendaItemResponse(
                a.getId(),
                patientId,
                buildName(person),
                buildInitials(person),
                a.getScheduledAt(),
                a.getDurationMinutes(),
                a.getReason(),
                a.getAppointmentTypeId() != null ? typeNames.get(a.getAppointmentTypeId()) : null,
                flowStatus,
                patientId != null && !returningPatientIds.contains(patientId)
        );
    }

    /** Promedio de minutos entre inicio y fin de consulta de las citas de hoy ya atendidas. */
    private long averageConsultationMinutes(List<Appointment> appointments) {
        List<Long> durations = appointments.stream()
                .filter(a -> a.getStartedAt() != null)
                .map(a -> {
                    LocalDateTime end = a.getCompletedAt() != null ? a.getCompletedAt() : a.getFinishedAt();
                    return end != null ? Duration.between(a.getStartedAt(), end).toMinutes() : null;
                })
                .filter(m -> m != null && m >= 0)
                .collect(Collectors.toList());

        if (durations.isEmpty()) {
            return 0;
        }
        return Math.round(durations.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private List<DoctorRecentPatientResponse> buildRecentPatients(Long tenantId, Long doctorId,
                                                                  LocalDateTime before, Map<Long, String> typeNames) {
        // Traemos más de las necesarias porque deduplicamos por paciente conservando la más reciente.
        List<Appointment> completed = appointmentRepository.findDoctorCompletedBefore(
                tenantId, doctorId, before, PageRequest.of(0, RECENT_PATIENTS_LIMIT * 4));

        List<DoctorRecentPatientResponse> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Appointment a : completed) {
            Patient patient = a.getPatient();
            if (patient == null || !seen.add(patient.getId())) {
                continue;
            }
            Person person = patient.getPerson();
            result.add(new DoctorRecentPatientResponse(
                    patient.getId(),
                    buildName(person),
                    buildInitials(person),
                    a.getCompletedAt() != null ? a.getCompletedAt() : a.getScheduledAt(),
                    a.getReason()
            ));
            if (result.size() >= RECENT_PATIENTS_LIMIT) {
                break;
            }
        }
        return result;
    }

    private String buildName(Person person) {
        if (person == null) {
            return null;
        }
        String first = person.getFirstName() != null ? person.getFirstName() : "";
        String last = person.getLastName() != null ? person.getLastName() : "";
        return (first + " " + last).trim();
    }

    private String buildInitials(Person person) {
        if (person == null) {
            return "";
        }
        String f = (person.getFirstName() != null && !person.getFirstName().isEmpty())
                ? String.valueOf(person.getFirstName().charAt(0)).toUpperCase() : "";
        String l = (person.getLastName() != null && !person.getLastName().isEmpty())
                ? String.valueOf(person.getLastName().charAt(0)).toUpperCase() : "";
        return f + l;
    }
}
