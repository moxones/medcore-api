package com.medical.medcore.service.triage.impl;

import com.medical.medcore.config.audit.ClinicalAuditContext;
import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.TriageRequest;
import com.medical.medcore.dto.response.TriageResponse;
import com.medical.medcore.dto.response.TriageSummaryResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.AppointmentType;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.Patient;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.Triage;
import com.medical.medcore.entity.User;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.AppointmentTypeRepository;
import com.medical.medcore.repository.TriageRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.service.triage.TriageService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
public class TriageServiceImpl implements TriageService {

    private final TriageRepository triageRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final UserRepository userRepository;
    private final ClinicalAuditContext clinicalAuditContext;

    @Override
    @Transactional
    public TriageResponse create(TriageRequest request) {
        clinicalAuditContext.apply();
        Long tenantId = TenantContext.requireTenantId();

        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        if (!appointment.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }

        validate(request);

        Triage triage = new Triage();
        triage.setAppointment(appointment);
        triage.setWeight(request.weight());
        triage.setHeight(request.height());
        triage.setTemperature(request.temperature());
        triage.setHeartRate(request.heartRate());
        triage.setRespiratoryRate(request.respiratoryRate());
        triage.setOxygenSaturation(request.oxygenSaturation());
        triage.setBloodPressure(request.bloodPressure());
        triage.setSystolicPressure(request.systolicPressure());
        triage.setDiastolicPressure(request.diastolicPressure());
        triage.setPainScale(request.painScale());
        triage.setBloodGlucose(request.bloodGlucose());
        triage.setBmi(resolveBmi(request));
        triage.setPriorityLevel(request.priorityLevel());
        triage.setPrioritySystem(request.prioritySystem() != null ? request.prioritySystem() : "ESI");
        triage.setNotes(request.notes());
        triage.setMeasuredAt(request.measuredAt());

        Long currentUserId = TenantContext.getCurrentUserId();
        triage.setCreatedBy(currentUserId);

        triage = triageRepository.save(triage);
        return mapToResponse(triage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriageResponse> listByAppointment(Long appointmentId) {
        Long tenantId = TenantContext.requireTenantId();
        return triageRepository.findByAppointmentIdOrderByMeasuredAtDescIdDesc(appointmentId)
                .stream()
                .filter(t -> t.getAppointment().getTenantId().equals(tenantId))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TriageResponse latestByAppointment(Long appointmentId) {
        Long tenantId = TenantContext.requireTenantId();
        Triage triage = triageRepository.findFirstByAppointmentIdOrderByMeasuredAtDescIdDesc(appointmentId)
                .orElseThrow(() -> new NotFoundException("Triaje no encontrado"));
        if (!triage.getAppointment().getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a este triaje");
        }
        return mapToResponse(triage);
    }

    @Override
    @Transactional(readOnly = true)
    public TriageResponse findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Triage triage = triageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Triaje no encontrado"));
        if (!triage.getAppointment().getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a este triaje");
        }
        return mapToResponse(triage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriageSummaryResponse> getDaySummary(Long doctorId, LocalDate date) {
        Long tenantId = TenantContext.requireTenantId();
        LocalDate target = date != null ? date : LocalDate.now();
        LocalDateTime start = target.atStartOfDay();
        LocalDateTime end = target.plusDays(1).atStartOfDay();

        List<Triage> triages = triageRepository.findDaySummary(tenantId, start, end, doctorId);

        Map<Long, String> typeNames = appointmentTypeRepository.findAll().stream()
                .collect(Collectors.toMap(AppointmentType::getId, AppointmentType::getName, (a, b) -> a));
        Map<Long, String> assistantNames = resolveAssistantNames(triages, tenantId);

        return triages.stream()
                .map(t -> toSummary(t, typeNames, assistantNames))
                .toList();
    }

    private Map<Long, String> resolveAssistantNames(List<Triage> triages, Long tenantId) {
        List<Long> userIds = triages.stream()
                .map(Triage::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .filter(u -> tenantId.equals(u.getTenantId()))
                .collect(Collectors.toMap(User::getId, u -> buildName(u.getPerson())));
    }

    private TriageSummaryResponse toSummary(Triage t, Map<Long, String> typeNames,
                                            Map<Long, String> assistantNames) {
        Appointment a = t.getAppointment();
        Patient patient = a.getPatient();
        Person patientPerson = patient != null ? patient.getPerson() : null;
        Doctor doctor = a.getDoctor();
        Person doctorPerson = doctor != null ? doctor.getPerson() : null;

        LocalDate birthDate = patientPerson != null ? patientPerson.getBirthDate() : null;

        return new TriageSummaryResponse(
                t.getId(),
                a.getId(),
                patient != null ? patient.getId() : null,
                buildName(patientPerson),
                patientPerson != null ? patientPerson.getPhone() : null,
                birthDate,
                calculateAge(birthDate),
                patientPerson != null ? patientPerson.getGender() : null,
                patient != null ? patient.getBloodType() : null,
                patient != null ? patient.getAllergies() : null,
                patient != null ? patient.getChronicConditions() : null,
                doctor != null ? doctor.getId() : null,
                buildName(doctorPerson),
                a.getAppointmentTypeId() != null ? typeNames.get(a.getAppointmentTypeId()) : null,
                a.getScheduledAt(),
                t.getCreatedAt(),
                t.getPriorityLevel(),
                t.getWeight(),
                t.getHeight(),
                t.getTemperature(),
                t.getHeartRate(),
                t.getBloodPressure(),
                t.getOxygenSaturation(),
                t.getRespiratoryRate(),
                t.getPainScale(),
                t.getNotes(),
                t.getCreatedBy() != null ? assistantNames.get(t.getCreatedBy()) : null
        );
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private String buildName(Person person) {
        if (person == null) {
            return null;
        }
        String first = person.getFirstName() != null ? person.getFirstName() : "";
        String last = person.getLastName() != null ? person.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }

    private void validate(TriageRequest request) {
        if (request.painScale() != null && (request.painScale() < 0 || request.painScale() > 10)) {
            throw new BadRequestException("pain_scale debe estar entre 0 y 10");
        }
        if (request.oxygenSaturation() != null
                && (request.oxygenSaturation().compareTo(BigDecimal.ZERO) < 0
                || request.oxygenSaturation().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BadRequestException("oxygen_saturation debe estar entre 0 y 100");
        }
        if (request.prioritySystem() != null
                && !request.prioritySystem().equals("ESI")
                && !request.prioritySystem().equals("MANCHESTER")) {
            throw new BadRequestException("priority_system debe ser ESI o MANCHESTER");
        }
    }

    private BigDecimal resolveBmi(TriageRequest request) {
        if (request.bmi() != null) return request.bmi();
        BigDecimal weight = request.weight();
        BigDecimal height = request.height();
        if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal meters = height.compareTo(BigDecimal.valueOf(3)) > 0
                ? height.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : height;
        BigDecimal squared = meters.multiply(meters);
        if (squared.compareTo(BigDecimal.ZERO) == 0) return null;
        return weight.divide(squared, 2, RoundingMode.HALF_UP);
    }

    private TriageResponse mapToResponse(Triage t) {
        return new TriageResponse(
                t.getId(),
                t.getAppointment().getId(),
                t.getWeight(),
                t.getHeight(),
                t.getTemperature(),
                t.getHeartRate(),
                t.getRespiratoryRate(),
                t.getOxygenSaturation(),
                t.getBloodPressure(),
                t.getSystolicPressure(),
                t.getDiastolicPressure(),
                t.getPainScale(),
                t.getBloodGlucose(),
                t.getBmi(),
                t.getPriorityLevel(),
                t.getPrioritySystem(),
                t.getNotes(),
                t.getMeasuredAt(),
                t.getCreatedAt(),
                t.getCreatedBy()
        );
    }
}
