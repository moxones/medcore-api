package com.medical.medcore.service.doctor;

import com.medical.medcore.dto.response.DoctorOrderResponse;
import com.medical.medcore.dto.response.DoctorPatientResponse;
import com.medical.medcore.dto.response.DoctorProfileResponse;
import com.medical.medcore.dto.response.MedicalEntryResponse.OrderResultItem;
import com.medical.medcore.dto.response.PrescriptionDocumentResponse;
import com.medical.medcore.dto.response.PrescriptionResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.MedicalEntry;
import com.medical.medcore.entity.MedicalEntryDiagnosis;
import com.medical.medcore.entity.MedicalOrder;
import com.medical.medcore.entity.MedicalOrderResult;
import com.medical.medcore.entity.Patient;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.Prescription;
import com.medical.medcore.entity.User;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.DoctorBranchRepository;
import com.medical.medcore.repository.DoctorSpecialtyRepository;
import com.medical.medcore.repository.MedicalEntryDiagnosisRepository;
import com.medical.medcore.repository.MedicalEntryRepository;
import com.medical.medcore.repository.MedicalOrderRepository;
import com.medical.medcore.repository.MedicalOrderResultRepository;
import com.medical.medcore.repository.PatientAllergyRepository;
import com.medical.medcore.repository.PatientConditionRepository;
import com.medical.medcore.repository.PersonDocumentRepository;
import com.medical.medcore.repository.PrescriptionRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.types.PageableResponse;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorPortalService {

    private final DoctorService doctorService;
    private final AppointmentRepository appointmentRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final PersonDocumentRepository personDocumentRepository;
    private final UserRepository userRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientConditionRepository patientConditionRepository;
    private final MedicalEntryRepository medicalEntryRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalEntryDiagnosisRepository diagnosisRepository;
    private final MedicalOrderRepository orderRepository;
    private final MedicalOrderResultRepository orderResultRepository;

    private static final Long STATUS_CANCELLED = 4L;

    @Transactional(readOnly = true)
    public DoctorProfileResponse getProfile() {
        Long tenantId = TenantContext.requireTenantId();
        Doctor doctor = doctorService.findMe();
        Long doctorId = doctor.getId();
        Person person = doctor.getPerson();

        String email = null;
        if (person != null) {
            email = userRepository.findByPersonIdAndTenantId(person.getId(), tenantId)
                    .map(User::getEmail)
                    .orElse(person.getContactEmail());
        }

        String documentNumber = person != null
                ? personDocumentRepository.findFirstByPersonId(person.getId())
                        .map(pd -> pd.getDocumentNumber()).orElse(null)
                : null;

        List<String> specialties = doctorSpecialtyRepository.findByDoctorIdFetchSpecialty(doctorId).stream()
                .map(ds -> ds.getSpecialty().getName())
                .toList();

        List<String> branches = doctorBranchRepository.findActiveBranchesByDoctorId(doctorId).stream()
                .map(db -> db.getBranch().getName())
                .toList();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        long totalPatients = appointmentRepository.countDistinctPatientsByDoctor(tenantId, doctorId);
        long appointmentsThisMonth = appointmentRepository.countByDoctorAndDateRange(
                tenantId, doctorId, startOfMonth, startOfNextMonth, STATUS_CANCELLED);
        long consultationsCompleted = appointmentRepository.countCompletedByDoctor(tenantId, doctorId);
        long avgConsultationMinutes = averageConsultationMinutes(
                appointmentRepository.findConsultationTimingsByDoctor(tenantId, doctorId));

        int seniorityYears = doctor.getCreatedAt() != null
                ? Math.max(0, Period.between(doctor.getCreatedAt().toLocalDate(), today).getYears())
                : 0;

        return new DoctorProfileResponse(
                doctorId,
                person != null ? person.getId() : null,
                buildName(person),
                buildInitials(person),
                email,
                person != null ? person.getPhone() : null,
                documentNumber,
                doctor.getLicenseNumber(),
                doctor.getIsActive(),
                seniorityYears,
                doctor.getCreatedAt(),
                specialties,
                branches,
                totalPatients,
                appointmentsThisMonth,
                consultationsCompleted,
                avgConsultationMinutes
        );
    }

    @Transactional(readOnly = true)
    public PageableResponse<DoctorPatientResponse> getPatients(String q, int page, int size) {
        Long tenantId = TenantContext.requireTenantId();
        Doctor doctor = doctorService.findMe();
        Long doctorId = doctor.getId();

        String term = (q == null || q.isBlank()) ? null : q.trim();
        Page<Patient> patients = appointmentRepository.findDistinctPatientsByDoctor(
                tenantId, doctorId, term, PageRequest.of(page, size));

        List<Long> patientIds = patients.getContent().stream().map(Patient::getId).toList();
        if (patientIds.isEmpty()) {
            return PageableResponse.from(patients.map(p -> null));
        }

        Map<Long, Long> allergyCounts = toCountMap(patientAllergyRepository.countActiveByPatientIds(patientIds));
        Map<Long, Long> conditionCounts = toCountMap(patientConditionRepository.countByPatientIds(patientIds));

        List<Appointment> visits = appointmentRepository.findByDoctorAndPatientIds(
                tenantId, doctorId, patientIds, STATUS_CANCELLED);
        Map<Long, List<Appointment>> visitsByPatient = visits.stream()
                .collect(Collectors.groupingBy(a -> a.getPatient().getId()));

        LocalDateTime now = LocalDateTime.now();

        return PageableResponse.from(patients.map(p ->
                toPatientResponse(p, allergyCounts, conditionCounts, visitsByPatient, now)));
    }

    private DoctorPatientResponse toPatientResponse(Patient patient,
                                                    Map<Long, Long> allergyCounts,
                                                    Map<Long, Long> conditionCounts,
                                                    Map<Long, List<Appointment>> visitsByPatient,
                                                    LocalDateTime now) {
        Person person = patient.getPerson();
        Long patientId = patient.getId();
        List<Appointment> visits = visitsByPatient.getOrDefault(patientId, List.of());

        Appointment lastVisit = null;
        Appointment nextAppointment = null;
        for (Appointment a : visits) {
            if (a.getScheduledAt() == null) continue;
            if (!a.getScheduledAt().isAfter(now)) {
                if (lastVisit == null || a.getScheduledAt().isAfter(lastVisit.getScheduledAt())) {
                    lastVisit = a;
                }
            } else {
                if (nextAppointment == null || a.getScheduledAt().isBefore(nextAppointment.getScheduledAt())) {
                    nextAppointment = a;
                }
            }
        }

        LocalDate birthDate = person != null ? person.getBirthDate() : null;
        Integer age = birthDate != null ? Period.between(birthDate, LocalDate.now()).getYears() : null;

        return DoctorPatientResponse.builder()
                .patientId(patientId)
                .fullName(buildName(person))
                .initials(buildInitials(person))
                .gender(person != null ? person.getGender() : null)
                .birthDate(birthDate != null ? birthDate.toString() : null)
                .age(age)
                .phone(person != null ? person.getPhone() : null)
                .email(person != null ? person.getContactEmail() : null)
                .bloodType(patient.getBloodType())
                .allergyCount(allergyCounts.getOrDefault(patientId, 0L).intValue())
                .conditionCount(conditionCounts.getOrDefault(patientId, 0L).intValue())
                .totalVisits((long) visits.size())
                .lastVisitAt(lastVisit != null ? lastVisit.getScheduledAt() : null)
                .lastReason(lastVisit != null ? lastVisit.getReason() : null)
                .nextAppointmentAt(nextAppointment != null ? nextAppointment.getScheduledAt() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public PageableResponse<PrescriptionDocumentResponse> getPrescriptions(String q, int page, int size) {
        Long tenantId = TenantContext.requireTenantId();
        Doctor doctor = doctorService.findMe();
        Long doctorId = doctor.getId();

        String term = (q == null || q.isBlank()) ? null : q.trim();
        Page<MedicalEntry> entries = medicalEntryRepository.findPrescriptionEntriesByDoctor(
                tenantId, doctorId, term, PageRequest.of(page, size));

        List<Long> entryIds = entries.getContent().stream().map(MedicalEntry::getId).toList();
        if (entryIds.isEmpty()) {
            return PageableResponse.from(entries.map(e -> null));
        }

        Map<Long, List<Prescription>> presByEntry = prescriptionRepository.findByMedicalEntryIdIn(entryIds)
                .stream().collect(Collectors.groupingBy(p -> p.getMedicalEntry().getId()));

        Map<Long, String> diagnosisSummaries = diagnosisRepository.findByMedicalEntryIdInOrderByIdAsc(entryIds)
                .stream().collect(Collectors.groupingBy(
                        d -> d.getMedicalEntry().getId(),
                        Collectors.mapping(MedicalEntryDiagnosis::getDescription,
                                Collectors.joining(", "))));

        return PageableResponse.from(entries.map(e ->
                toPrescriptionDocument(e, presByEntry.getOrDefault(e.getId(), List.of()),
                        diagnosisSummaries.get(e.getId()))));
    }

    private PrescriptionDocumentResponse toPrescriptionDocument(MedicalEntry entry,
                                                                List<Prescription> prescriptions,
                                                                String diagnosisSummary) {
        Patient patient = entry.getMedicalRecord() != null ? entry.getMedicalRecord().getPatient() : null;
        Person person = patient != null ? patient.getPerson() : null;

        String summary = (diagnosisSummary != null && !diagnosisSummary.isBlank())
                ? diagnosisSummary
                : entry.getDiagnosis();

        List<PrescriptionResponse> items = prescriptions.stream()
                .map(p -> new PrescriptionResponse(
                        p.getId(), p.getMedication(), p.getDosage(), p.getFrequency(), p.getDuration(),
                        p.getRoute(), p.getQuantity(), p.getPresentation(), p.getIsActive(), p.getInstructions()))
                .toList();

        return new PrescriptionDocumentResponse(
                entry.getId(),
                entry.getAppointment() != null ? entry.getAppointment().getId() : null,
                patient != null ? patient.getId() : null,
                buildName(person),
                buildInitials(person),
                entry.getCreatedAt(),
                summary,
                entry.getIsLocked(),
                items
        );
    }

    @Transactional(readOnly = true)
    public List<DoctorOrderResponse> getOrders(String status) {
        Long tenantId = TenantContext.requireTenantId();
        Doctor doctor = doctorService.findMe();
        Long doctorId = doctor.getId();

        String statusFilter = (status == null || status.isBlank()) ? null : status.trim();
        List<MedicalOrder> orders = orderRepository.findByDoctor(tenantId, doctorId, statusFilter);
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(MedicalOrder::getId).toList();
        Map<Long, List<OrderResultItem>> resultsByOrder = orderResultRepository
                .findByMedicalOrderIdInOrderByIdAsc(orderIds).stream()
                .collect(Collectors.groupingBy(r -> r.getMedicalOrder().getId(),
                        Collectors.mapping(this::mapResult, Collectors.toList())));

        return orders.stream().map(o -> toOrderResponse(o, resultsByOrder)).toList();
    }

    private DoctorOrderResponse toOrderResponse(MedicalOrder order,
                                                Map<Long, List<OrderResultItem>> resultsByOrder) {
        MedicalEntry entry = order.getMedicalEntry();
        Patient patient = entry != null && entry.getMedicalRecord() != null
                ? entry.getMedicalRecord().getPatient() : null;
        Person person = patient != null ? patient.getPerson() : null;

        return new DoctorOrderResponse(
                order.getId(),
                order.getOrderType(),
                order.getDescription(),
                order.getStatus(),
                order.getRequestedAt(),
                resultsByOrder.getOrDefault(order.getId(), List.of()),
                entry != null ? entry.getId() : null,
                entry != null && entry.getAppointment() != null ? entry.getAppointment().getId() : null,
                patient != null ? patient.getId() : null,
                buildName(person),
                buildInitials(person)
        );
    }

    private OrderResultItem mapResult(MedicalOrderResult r) {
        return new OrderResultItem(r.getId(), r.getResult(), r.getFileUrl(), r.getResultDate());
    }

    private long averageConsultationMinutes(List<Object[]> timings) {
        List<Long> durations = timings.stream()
                .map(row -> {
                    LocalDateTime started = (LocalDateTime) row[0];
                    LocalDateTime finished = (LocalDateTime) row[1];
                    LocalDateTime completed = (LocalDateTime) row[2];
                    LocalDateTime end = completed != null ? completed : finished;
                    if (started == null || end == null) return null;
                    return Duration.between(started, end).toMinutes();
                })
                .filter(m -> m != null && m >= 0)
                .toList();
        if (durations.isEmpty()) return 0;
        return Math.round(durations.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyMap();
        return rows.stream().collect(Collectors.toMap(
                r -> (Long) r[0],
                r -> (Long) r[1],
                (a, b) -> a));
    }

    private String buildName(Person person) {
        if (person == null) return null;
        String first = person.getFirstName() != null ? person.getFirstName() : "";
        String last = person.getLastName() != null ? person.getLastName() : "";
        return (first + " " + last).trim();
    }

    private String buildInitials(Person person) {
        if (person == null) return "";
        String f = (person.getFirstName() != null && !person.getFirstName().isEmpty())
                ? String.valueOf(person.getFirstName().charAt(0)).toUpperCase() : "";
        String l = (person.getLastName() != null && !person.getLastName().isEmpty())
                ? String.valueOf(person.getLastName().charAt(0)).toUpperCase() : "";
        return f + l;
    }
}
