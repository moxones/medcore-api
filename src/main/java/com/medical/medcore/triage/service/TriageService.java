package com.medical.medcore.triage.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.domain.AppointmentFlowStatus;
import com.medical.medcore.appointment.service.AppointmentAccess;
import com.medical.medcore.appointment.service.AppointmentService;
import com.medical.medcore.appointment.service.BookingPolicy;
import com.medical.medcore.catalog.domain.AppointmentType;
import com.medical.medcore.catalog.repository.AppointmentTypeRepository;
import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.repository.PatientRepository;
import com.medical.medcore.shared.audit.ClinicalAuditContext;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.BranchScope;
import com.medical.medcore.shared.tenancy.TenantContext;
import com.medical.medcore.triage.domain.Triage;
import com.medical.medcore.triage.dto.TriageRequest;
import com.medical.medcore.triage.dto.TriageResponse;
import com.medical.medcore.triage.dto.TriageSummaryResponse;
import com.medical.medcore.triage.repository.TriageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TriageService {

    private static final String DEFAULT_PRIORITY_SYSTEM = "ESI";
    private static final BigDecimal CENTIMETERS_THRESHOLD = BigDecimal.valueOf(3);
    private static final int PATIENT_HISTORY_LIMIT = 50;
    private static final String NOT_FOUND = "Triaje no encontrado";

    private final TriageRepository triageRepository;
    private final AppointmentService appointmentService;
    private final AppointmentAccess appointmentAccess;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final BookingPolicy bookingPolicy;
    private final ClinicalAuditContext clinicalAuditContext;

    @Transactional
    public TriageResponse create(TriageRequest request) {
        clinicalAuditContext.apply();
        Appointment appointment = appointmentService.loadAccessible(request.appointmentId());
        appointmentService.assertScheduled(appointment);
        AppointmentFlowStatus flow = appointment.flow();
        if (flow == AppointmentFlowStatus.SCHEDULED) {
            throw new ConflictException("ARRIVAL_NOT_REGISTERED",
                    "Registra la llegada del paciente antes de tomar el triaje");
        }
        if (flow.isAttended()) {
            throw new ConflictException("APPOINTMENT_NOT_ACTIVE", "La atención de esta cita ya finalizó");
        }
        LocalDateTime now = bookingPolicy.now();
        LocalDateTime measuredAt = request.measuredAt() != null ? request.measuredAt() : now;
        if (measuredAt.isAfter(now.plusMinutes(5))) {
            throw new BadRequestException("La hora de medición no puede ser futura");
        }
        if (request.systolicPressure() != null && request.diastolicPressure() != null
                && request.diastolicPressure() >= request.systolicPressure()) {
            throw new BadRequestException("La presión diastólica debe ser menor que la sistólica");
        }
        Triage triage = triageRepository.save(Triage.builder()
                .tenantId(appointment.getTenantId())
                .appointment(appointment)
                .weight(request.weight())
                .height(request.height())
                .temperature(request.temperature())
                .heartRate(request.heartRate())
                .respiratoryRate(request.respiratoryRate())
                .oxygenSaturation(request.oxygenSaturation())
                .bloodPressure(resolveBloodPressure(request))
                .systolicPressure(request.systolicPressure())
                .diastolicPressure(request.diastolicPressure())
                .painScale(request.painScale())
                .bloodGlucose(request.bloodGlucose())
                .bmi(resolveBmi(request))
                .priorityLevel(request.priorityLevel())
                .prioritySystem(request.prioritySystem() != null ? request.prioritySystem() : DEFAULT_PRIORITY_SYSTEM)
                .notes(request.notes())
                .measuredAt(measuredAt)
                .build());
        return toResponse(triage);
    }

    public List<TriageResponse> listByAppointment(Long appointmentId) {
        Appointment appointment = appointmentService.loadAccessible(appointmentId);
        return triageRepository.findByAppointmentIdOrderByMeasuredAtDescIdDesc(appointment.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public TriageResponse latestByAppointment(Long appointmentId) {
        Appointment appointment = appointmentService.loadAccessible(appointmentId);
        return triageRepository.findFirstByAppointmentIdOrderByMeasuredAtDescIdDesc(appointment.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    public TriageResponse findById(Long id) {
        Triage triage = triageRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        appointmentService.loadAccessible(triage.getAppointment().getId());
        return toResponse(triage);
    }

    public List<TriageSummaryResponse> getDaySummary(Long branchId, Long doctorId, LocalDate date, String urgency) {
        Long tenantId = TenantContext.requireTenantId();
        LocalDate target = date != null ? date : bookingPolicy.today();
        BranchScope scope = BranchScope.resolve(branchId);
        if (scope.seesNothing()) {
            return List.of();
        }
        List<Triage> triages = triageRepository.findDaySummary(tenantId, target.atStartOfDay(),
                target.plusDays(1).atStartOfDay(), appointmentAccess.restrictDoctorFilter(doctorId),
                scope.restricted(), scope.queryIds());
        return summarize(triages, tenantId).stream()
                .filter(summary -> urgency == null || urgency.isBlank() || urgency.equalsIgnoreCase(summary.urgencyLevel()))
                .toList();
    }

    public List<TriageSummaryResponse> listByPatient(Long patientId) {
        Long tenantId = TenantContext.requireTenantId();
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));
        List<Triage> triages = triageRepository.findByPatient(tenantId, patient.getId(),
                PageRequest.of(0, PATIENT_HISTORY_LIMIT));
        return summarize(triages, tenantId);
    }

    private List<TriageSummaryResponse> summarize(List<Triage> triages, Long tenantId) {
        if (triages.isEmpty()) {
            return List.of();
        }
        Map<Long, String> typeNames = appointmentTypeRepository.findAll().stream()
                .collect(Collectors.toMap(AppointmentType::getId, AppointmentType::getName));
        List<Long> authorIds = triages.stream().map(Triage::getCreatedBy).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> authorNames = userRepository.findByIdInAndTenantId(authorIds, tenantId).stream()
                .collect(Collectors.toMap(User::getId, user -> user.getPerson().fullName() != null
                        ? user.getPerson().fullName() : user.getEmail()));
        return triages.stream().map(triage -> toSummary(triage, typeNames, authorNames)).toList();
    }

    private TriageSummaryResponse toSummary(Triage triage, Map<Long, String> typeNames, Map<Long, String> authorNames) {
        Appointment appointment = triage.getAppointment();
        Patient patient = appointment.getPatient();
        Person person = patient.getPerson();
        return new TriageSummaryResponse(
                triage.getId(),
                appointment.getId(),
                patient.getId(),
                person.fullName(),
                person.getPhone(),
                person.getBirthDate(),
                ageOf(person.getBirthDate()),
                person.getGender(),
                patient.getBloodType(),
                patient.getAllergies(),
                patient.getChronicConditions(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getPerson().fullName(),
                appointment.getAppointmentTypeId() != null ? typeNames.get(appointment.getAppointmentTypeId()) : null,
                appointment.getScheduledAt(),
                triage.getCreatedAt(),
                TriageUrgency.from(triage.getPrioritySystem(), triage.getPriorityLevel()).name(),
                triage.getWeight(),
                triage.getHeight(),
                triage.getTemperature(),
                triage.getHeartRate(),
                triage.getBloodPressure(),
                triage.getOxygenSaturation(),
                triage.getRespiratoryRate(),
                triage.getPainScale(),
                triage.getNotes(),
                triage.getCreatedBy() != null ? authorNames.get(triage.getCreatedBy()) : null);
    }

    private Integer ageOf(LocalDate birthDate) {
        LocalDate today = bookingPolicy.today();
        if (birthDate == null || birthDate.isAfter(today)) {
            return null;
        }
        return Period.between(birthDate, today).getYears();
    }

    private String resolveBloodPressure(TriageRequest request) {
        if (request.bloodPressure() != null && !request.bloodPressure().isBlank()) {
            return request.bloodPressure().trim();
        }
        if (request.systolicPressure() != null && request.diastolicPressure() != null) {
            return request.systolicPressure() + "/" + request.diastolicPressure();
        }
        return null;
    }

    private BigDecimal resolveBmi(TriageRequest request) {
        if (request.bmi() != null) {
            return request.bmi();
        }
        BigDecimal weight = request.weight();
        BigDecimal height = request.height();
        if (weight == null || height == null) {
            return null;
        }
        BigDecimal meters = height.compareTo(CENTIMETERS_THRESHOLD) > 0
                ? height.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : height;
        BigDecimal bmi = weight.divide(meters.multiply(meters), 2, RoundingMode.HALF_UP);
        return bmi.compareTo(BigDecimal.valueOf(999)) > 0 ? null : bmi;
    }

    private TriageResponse toResponse(Triage triage) {
        return new TriageResponse(
                triage.getId(),
                triage.getAppointment().getId(),
                triage.getWeight(),
                triage.getHeight(),
                triage.getTemperature(),
                triage.getHeartRate(),
                triage.getRespiratoryRate(),
                triage.getOxygenSaturation(),
                triage.getBloodPressure(),
                triage.getSystolicPressure(),
                triage.getDiastolicPressure(),
                triage.getPainScale(),
                triage.getBloodGlucose(),
                triage.getBmi(),
                triage.getPriorityLevel(),
                triage.getPrioritySystem(),
                triage.getNotes(),
                triage.getMeasuredAt(),
                triage.getCreatedAt(),
                triage.getCreatedBy());
    }
}
