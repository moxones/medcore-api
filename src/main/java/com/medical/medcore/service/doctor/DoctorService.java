package com.medical.medcore.service.doctor;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.response.DoctorPatientResponse;
import com.medical.medcore.dto.response.DoctorSelfResponse;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.Patient;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.User;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.repository.PatientAllergyRepository;
import com.medical.medcore.repository.PatientConditionRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.types.PageableResponse;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private static final long STATUS_CANCELLED = 4L;

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientConditionRepository patientConditionRepository;

    /** Resuelve el doctor asociado al usuario autenticado. */
    public Doctor findMe() {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return doctorRepository.findByPersonIdAndTenantId(user.getPerson().getId(), tenantId)
                .orElseThrow(() -> new NotFoundException("No existe un médico asociado a este usuario"));
    }

    /** Vista pública del médico autenticado, sin exponer la entidad JPA. */
    public DoctorSelfResponse getMyProfile() {
        return DoctorSelfResponse.from(findMe());
    }

    public PageableResponse<Doctor> findAll(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant no disponible");
        }
        return PageableResponse.from(doctorRepository.findByTenantIdAndIsActiveTrue(tenantId, PageRequest.of(page, size)));
    }

    public Doctor findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant no disponible");
        }
        return doctorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado"));
    }

    public Doctor create(Doctor doctor) {
        Long tenantId = TenantContext.requireTenantId();
        doctor.setTenantId(tenantId);
        return doctorRepository.save(doctor);
    }

    public Doctor update(Long id, Doctor doctor) {
        Long tenantId = TenantContext.requireTenantId();
        Doctor existing = doctorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Médico no encontrado"));
        if (doctor.getLicenseNumber() != null) {
            existing.setLicenseNumber(doctor.getLicenseNumber());
        }
        if (doctor.getIsActive() != null) {
            existing.setIsActive(doctor.getIsActive());
        }
        return doctorRepository.save(existing);
    }

    public void deactivate(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Doctor doctor = doctorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Médico no encontrado"));
        doctor.setIsActive(false);
        doctorRepository.save(doctor);
    }

    public PageableResponse<DoctorPatientResponse> getMyPatients(String q, int page, int size) {
        Long tenantId = TenantContext.requireTenantId();
        Doctor doctor = findMe();
        String queryParam = (q != null && !q.isBlank()) ? q.trim() : null;

        Page<Patient> patientPage = appointmentRepository.findDistinctPatientsByDoctor(
                tenantId, doctor.getId(), queryParam, PageRequest.of(page, size));

        List<Long> patientIds = patientPage.getContent().stream()
                .map(Patient::getId).toList();

        if (patientIds.isEmpty()) {
            return PageableResponse.from(patientPage.map(p -> null));
        }

        Map<Long, Long> visitCounts = toVisitCountMap(
                appointmentRepository.countVisitsByPatientIds(tenantId, doctor.getId(), patientIds, STATUS_CANCELLED));
        Map<Long, Object[]> lastVisits = toLastVisitMap(
                appointmentRepository.findLastVisitsByPatientIds(tenantId, doctor.getId(), patientIds));
        Map<Long, LocalDateTime> nextAppointments = toNextAppointmentMap(
                appointmentRepository.findNextAppointmentsByPatientIds(tenantId, doctor.getId(), patientIds, STATUS_CANCELLED));
        Map<Long, Integer> allergyCounts = toIntCountMap(
                patientAllergyRepository.countActiveByPatientIds(patientIds));
        Map<Long, Integer> conditionCounts = toIntCountMap(
                patientConditionRepository.countByPatientIds(patientIds));

        return PageableResponse.from(patientPage.map(patient -> {
            Long pid = patient.getId();
            Person p = patient.getPerson();
            String firstName = p.getFirstName() != null ? p.getFirstName() : "";
            String lastName = p.getLastName() != null ? p.getLastName() : "";
            Object[] lastVisit = lastVisits.get(pid);

            return DoctorPatientResponse.builder()
                    .patientId(pid)
                    .fullName((firstName + " " + lastName).trim())
                    .initials(initials(firstName, lastName))
                    .gender(p.getGender())
                    .birthDate(p.getBirthDate() != null ? p.getBirthDate().toString() : null)
                    .age(p.getBirthDate() != null ? Period.between(p.getBirthDate(), LocalDate.now()).getYears() : null)
                    .phone(p.getPhone())
                    .email(p.getContactEmail())
                    .bloodType(patient.getBloodType())
                    .allergyCount(allergyCounts.getOrDefault(pid, 0))
                    .conditionCount(conditionCounts.getOrDefault(pid, 0))
                    .totalVisits(visitCounts.getOrDefault(pid, 0L))
                    .lastVisitAt(lastVisit != null ? (LocalDateTime) lastVisit[1] : null)
                    .lastReason(lastVisit != null ? (String) lastVisit[2] : null)
                    .nextAppointmentAt(nextAppointments.get(pid))
                    .build();
        }));
    }

    private static String initials(String firstName, String lastName) {
        String a = firstName.isEmpty() ? "" : String.valueOf(firstName.charAt(0)).toUpperCase();
        String b = lastName.isEmpty() ? "" : String.valueOf(lastName.charAt(0)).toUpperCase();
        return a + b;
    }

    private static Map<Long, Long> toVisitCountMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> ((Number) r[1]).longValue()));
    }

    private static Map<Long, Object[]> toLastVisitMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> new Object[]{r[0], toLocalDateTime(r[1]), r[2]}));
    }

    private static Map<Long, LocalDateTime> toNextAppointmentMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> toLocalDateTime(r[1])));
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof LocalDateTime ldt) return ldt;
        return null;
    }

    private static Map<Long, Integer> toIntCountMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> ((Number) r[1]).intValue()));
    }
}
