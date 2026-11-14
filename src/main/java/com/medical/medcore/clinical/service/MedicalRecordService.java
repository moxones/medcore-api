package com.medical.medcore.clinical.service;

import com.medical.medcore.appointment.service.AppointmentService;
import com.medical.medcore.appointment.service.BookingPolicy;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.security.SecurityUtils;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.repository.AppointmentRepository;
import com.medical.medcore.catalog.domain.Cie10Code;
import com.medical.medcore.catalog.repository.Cie10CodeRepository;
import com.medical.medcore.clinical.domain.MedicalCertificate;
import com.medical.medcore.clinical.domain.MedicalEntry;
import com.medical.medcore.clinical.domain.MedicalEntryDiagnosis;
import com.medical.medcore.clinical.domain.MedicalOrder;
import com.medical.medcore.clinical.domain.MedicalOrderResult;
import com.medical.medcore.clinical.domain.MedicalProcedure;
import com.medical.medcore.clinical.domain.MedicalRecord;
import com.medical.medcore.clinical.domain.Prescription;
import com.medical.medcore.clinical.repository.MedicalCertificateRepository;
import com.medical.medcore.clinical.repository.MedicalEntryDiagnosisRepository;
import com.medical.medcore.clinical.repository.MedicalEntryRepository;
import com.medical.medcore.clinical.repository.MedicalOrderRepository;
import com.medical.medcore.clinical.repository.MedicalOrderResultRepository;
import com.medical.medcore.clinical.repository.MedicalProcedureRepository;
import com.medical.medcore.clinical.repository.MedicalRecordRepository;
import com.medical.medcore.clinical.repository.PrescriptionRepository;
import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.repository.PatientRepository;

import com.medical.medcore.shared.audit.ClinicalAuditContext;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.clinical.dto.CertificateRequest;
import com.medical.medcore.clinical.dto.CreateMedicalEntryRequest;
import com.medical.medcore.clinical.dto.DiagnosisRequest;
import com.medical.medcore.clinical.dto.OrderRequest;
import com.medical.medcore.clinical.dto.OrderResultRequest;
import com.medical.medcore.clinical.dto.PrescriptionRequest;
import com.medical.medcore.clinical.dto.ProcedureRequest;
import com.medical.medcore.clinical.dto.UpdatePatientClinicalRequest;
import com.medical.medcore.clinical.dto.MedicalEntryResponse;
import com.medical.medcore.clinical.dto.MedicalEntryResponse.*;
import com.medical.medcore.clinical.dto.MedicalRecordResponse;
import com.medical.medcore.clinical.dto.PrescriptionResponse;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalEntryRepository medicalEntryRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicalEntryDiagnosisRepository diagnosisRepository;
    private final MedicalProcedureRepository procedureRepository;
    private final MedicalOrderRepository orderRepository;
    private final MedicalOrderResultRepository orderResultRepository;
    private final MedicalCertificateRepository certificateRepository;
    private final Cie10CodeRepository cie10CodeRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ClinicalAuditContext clinicalAuditContext;
    private final AppointmentService appointmentService;
    private final BookingPolicy bookingPolicy;

    @Transactional(readOnly = true)
    public MedicalRecordResponse getByPatientId(Long patientId) {
        Long tenantId = TenantContext.requireTenantId();
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));
        return buildRecordResponse(patient, tenantId);
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse getMyRecord() {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Patient patient = patientRepository.findByPersonIdAndTenantId(user.getPerson().getId(), tenantId)
                .orElseThrow(() -> new NotFoundException("No existe un paciente asociado a este usuario"));

        return buildRecordResponse(patient, tenantId);
    }

    @Transactional
    public MedicalEntryResponse addEntry(CreateMedicalEntryRequest request) {
        clinicalAuditContext.apply();
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();

        Appointment appointment = appointmentService.loadAccessible(request.appointmentId());
        if (appointment.flow().isBeforeConsultation()) {
            throw new ConflictException("CONSULTATION_NOT_STARTED",
                    "Inicia la consulta antes de registrar la atención en la historia clínica");
        }
        Patient patient = appointment.getPatient();
        MedicalRecord record = getOrCreateRecord(patient, tenantId, userId);
        String entryType = request.entryType() != null ? request.entryType() : "CONSULTATION";

        MedicalEntry entry = medicalEntryRepository.findByAppointmentIdAndTenantId(appointment.getId(), tenantId).stream()
                .filter(existing -> entryType.equals(existing.getEntryType()))
                .findFirst()
                .orElse(null);
        if (entry != null && Boolean.TRUE.equals(entry.getIsLocked())) {
            throw new ConflictException("ENTRY_LOCKED", "La nota de esta cita ya está firmada y no puede modificarse");
        }
        if (entry == null) {
            entry = MedicalEntry.builder()
                    .tenantId(tenantId)
                    .medicalRecord(record)
                    .appointment(appointment)
                    .doctorId(appointment.getDoctor().getId())
                    .entryType(entryType)
                    .isLocked(false)
                    .createdBy(userId)
                    .build();
        } else {
            assertAuthor(entry, userId);
            removeChildren(entry.getId());
            entry.setUpdatedBy(userId);
        }
        entry.setChiefComplaint(request.chiefComplaint());
        entry.setPresentIllness(request.presentIllness());
        entry.setReviewOfSystems(request.reviewOfSystems());
        entry.setPhysicalExamination(request.physicalExamination());
        entry.setAssessment(request.assessment());
        entry.setPlan(request.plan());
        entry.setDiagnosis(request.diagnosis());
        entry.setTreatment(request.treatment());
        entry.setNotes(request.notes());
        entry.setFollowUpAt(request.followUpAt());
        entry = medicalEntryRepository.save(entry);

        persistChildren(entry, request, userId);
        return mapEntries(List.of(entry), tenantId).get(0);
    }

    @Transactional(readOnly = true)
    public MedicalEntryResponse getEntry(Long entryId) {
        Long tenantId = TenantContext.requireTenantId();
        MedicalEntry entry = medicalEntryRepository.findByIdAndTenantId(entryId, tenantId)
                .orElseThrow(() -> new NotFoundException("Entrada de historia clínica no encontrada"));
        return mapEntries(List.of(entry), tenantId).get(0);
    }

    @Transactional(readOnly = true)
    public List<MedicalEntryResponse> getEntriesByAppointment(Long appointmentId) {
        Long tenantId = TenantContext.requireTenantId();
        appointmentService.loadAccessible(appointmentId);
        List<MedicalEntry> entries = medicalEntryRepository.findByAppointmentIdAndTenantId(appointmentId, tenantId);
        return mapEntries(entries, tenantId);
    }

    @Transactional
    public MedicalEntryResponse signEntry(Long entryId) {
        clinicalAuditContext.apply();
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();

        MedicalEntry entry = medicalEntryRepository.findByIdAndTenantId(entryId, tenantId)
                .orElseThrow(() -> new NotFoundException("Entrada de historia clínica no encontrada"));
        if (Boolean.TRUE.equals(entry.getIsLocked())) {
            throw new ConflictException("ENTRY_LOCKED", "La nota ya está firmada y bloqueada");
        }
        assertAuthor(entry, userId);
        entry.setSignedBy(userId);
        entry.setSignedAt(bookingPolicy.now());
        entry.setIsLocked(true);
        entry.setUpdatedBy(userId);
        entry = medicalEntryRepository.save(entry);
        return mapEntries(List.of(entry), tenantId).get(0);
    }

    @Transactional
    public MedicalEntryResponse.OrderItem addOrderResult(Long orderId, OrderResultRequest request) {
        clinicalAuditContext.apply();
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getCurrentUserId();

        MedicalOrder order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada"));

        MedicalOrderResult result = MedicalOrderResult.builder()
                .medicalOrder(order)
                .result(request.result())
                .fileUrl(request.fileUrl())
                .resultDate(request.resultDate() != null ? request.resultDate() : LocalDateTime.now())
                .createdBy(userId)
                .build();
        orderResultRepository.save(result);

        order.setStatus(request.status() != null ? request.status() : "COMPLETED");
        orderRepository.save(order);

        List<OrderResultItem> results = orderResultRepository.findByMedicalOrderIdOrderByIdAsc(order.getId())
                .stream().map(this::mapResult).toList();
        return new OrderItem(order.getId(), order.getOrderType(), order.getDescription(),
                order.getStatus(), order.getRequestedAt(), results);
    }

    @Transactional
    public MedicalRecordResponse updatePatientClinical(Long patientId, UpdatePatientClinicalRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));

        if (request.bloodType() != null) patient.setBloodType(request.bloodType());
        if (request.allergies() != null) patient.setAllergies(request.allergies());
        if (request.chronicConditions() != null) patient.setChronicConditions(request.chronicConditions());
        if (request.clinicalNotes() != null) patient.setClinicalNotes(request.clinicalNotes());

        patientRepository.save(patient);
        return buildRecordResponse(patient, tenantId);
    }

    private void persistChildren(MedicalEntry entry, CreateMedicalEntryRequest request, Long userId) {
        if (request.prescriptions() != null) {
            List<Prescription> prescriptions = new ArrayList<>();
            for (PrescriptionRequest pr : request.prescriptions()) {
                if (pr == null || pr.medication() == null || pr.medication().isBlank()) continue;
                prescriptions.add(Prescription.builder()
                        .medicalEntry(entry)
                        .medication(pr.medication())
                        .dosage(pr.dosage())
                        .frequency(pr.frequency())
                        .duration(pr.duration())
                        .route(pr.route())
                        .quantity(pr.quantity())
                        .presentation(pr.presentation())
                        .isActive(true)
                        .instructions(pr.instructions())
                        .build());
            }
            if (!prescriptions.isEmpty()) prescriptionRepository.saveAll(prescriptions);
        }

        if (request.diagnoses() != null) {
            List<MedicalEntryDiagnosis> diagnoses = new ArrayList<>();
            for (DiagnosisRequest dr : request.diagnoses()) {
                if (dr == null || dr.description() == null || dr.description().isBlank()) continue;
                diagnoses.add(MedicalEntryDiagnosis.builder()
                        .medicalEntry(entry)
                        .cie10Id(dr.cie10Id())
                        .description(dr.description())
                        .diagnosisType(dr.diagnosisType() != null ? dr.diagnosisType() : "DEFINITIVE")
                        .diagnosisRank(dr.diagnosisRank() != null ? dr.diagnosisRank() : "PRIMARY")
                        .notes(dr.notes())
                        .createdBy(userId)
                        .build());
            }
            if (!diagnoses.isEmpty()) diagnosisRepository.saveAll(diagnoses);
        }

        if (request.procedures() != null) {
            List<MedicalProcedure> procedures = new ArrayList<>();
            for (ProcedureRequest pr : request.procedures()) {
                if (pr == null || pr.name() == null || pr.name().isBlank()) continue;
                procedures.add(MedicalProcedure.builder()
                        .medicalEntry(entry)
                        .code(pr.code())
                        .name(pr.name())
                        .notes(pr.notes())
                        .performedAt(pr.performedAt())
                        .createdBy(userId)
                        .build());
            }
            if (!procedures.isEmpty()) procedureRepository.saveAll(procedures);
        }

        if (request.orders() != null) {
            List<MedicalOrder> orders = new ArrayList<>();
            for (OrderRequest or : request.orders()) {
                if (or == null || or.orderType() == null || or.orderType().isBlank()) continue;
                orders.add(MedicalOrder.builder()
                        .medicalEntry(entry)
                        .orderType(or.orderType())
                        .description(or.description())
                        .status("REQUESTED")
                        .createdBy(userId)
                        .build());
            }
            if (!orders.isEmpty()) orderRepository.saveAll(orders);
        }

        if (request.certificates() != null) {
            List<MedicalCertificate> certificates = new ArrayList<>();
            for (CertificateRequest cr : request.certificates()) {
                if (cr == null || cr.certificateType() == null || cr.certificateType().isBlank()) continue;
                certificates.add(MedicalCertificate.builder()
                        .medicalEntry(entry)
                        .certificateType(cr.certificateType())
                        .content(cr.content())
                        .restDays(cr.restDays())
                        .validUntil(cr.validUntil())
                        .createdBy(userId)
                        .build());
            }
            if (!certificates.isEmpty()) certificateRepository.saveAll(certificates);
        }
    }

    private void assertAuthor(MedicalEntry entry, Long userId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (entry.getCreatedBy() == null || !entry.getCreatedBy().equals(userId)) {
            throw new ForbiddenException("Solo el médico que registró la nota puede modificarla o firmarla");
        }
    }

    private void removeChildren(Long entryId) {
        prescriptionRepository.deleteByMedicalEntryId(entryId);
        diagnosisRepository.deleteByMedicalEntryId(entryId);
        procedureRepository.deleteByMedicalEntryId(entryId);
        orderRepository.deleteByMedicalEntryId(entryId);
        certificateRepository.deleteByMedicalEntryId(entryId);
    }

    private MedicalRecord getOrCreateRecord(Patient patient, Long tenantId, Long userId) {
        return medicalRecordRepository.findByPatientIdAndTenantId(patient.getId(), tenantId)
                .orElseGet(() -> {
                    MedicalRecord newRecord = MedicalRecord.builder()
                            .tenantId(tenantId)
                            .patient(patient)
                            .createdBy(userId)
                            .build();
                    return medicalRecordRepository.save(newRecord);
                });
    }

    private MedicalRecordResponse buildRecordResponse(Patient patient, Long tenantId) {
        Person person = patient.getPerson();
        String patientName = person != null
                ? ((person.getFirstName() != null ? person.getFirstName() : "") + " "
                + (person.getLastName() != null ? person.getLastName() : "")).trim()
                : null;

        MedicalRecord record = medicalRecordRepository
                .findByPatientIdAndTenantId(patient.getId(), tenantId)
                .orElse(null);

        List<MedicalEntryResponse> entryResponses = Collections.emptyList();
        Long recordId = null;
        LocalDateTime recordCreatedAt = null;

        if (record != null) {
            recordId = record.getId();
            recordCreatedAt = record.getCreatedAt();
            List<MedicalEntry> entries = medicalEntryRepository.findByMedicalRecordId(record.getId());
            entryResponses = mapEntries(entries, tenantId);
        }

        return new MedicalRecordResponse(
                recordId,
                patient.getId(),
                patientName,
                patient.getBloodType(),
                patient.getAllergies(),
                patient.getChronicConditions(),
                patient.getClinicalNotes(),
                recordCreatedAt,
                entryResponses
        );
    }

    private List<MedicalEntryResponse> mapEntries(List<MedicalEntry> entries, Long tenantId) {
        if (entries.isEmpty()) return Collections.emptyList();

        List<Long> entryIds = entries.stream().map(MedicalEntry::getId).toList();

        Map<Long, List<Prescription>> presByEntry = prescriptionRepository.findByMedicalEntryIdIn(entryIds)
                .stream().collect(Collectors.groupingBy(p -> p.getMedicalEntry().getId()));

        List<MedicalEntryDiagnosis> allDiagnoses = diagnosisRepository.findByMedicalEntryIdInOrderByIdAsc(entryIds);
        Map<Long, List<MedicalEntryDiagnosis>> diagByEntry = allDiagnoses.stream()
                .collect(Collectors.groupingBy(d -> d.getMedicalEntry().getId()));
        Map<Long, String> cie10Codes = resolveCie10Codes(allDiagnoses);

        Map<Long, List<MedicalProcedure>> procByEntry = procedureRepository.findByMedicalEntryIdInOrderByIdAsc(entryIds)
                .stream().collect(Collectors.groupingBy(p -> p.getMedicalEntry().getId()));

        List<MedicalOrder> allOrders = orderRepository.findByMedicalEntryIdInOrderByIdAsc(entryIds);
        Map<Long, List<MedicalOrder>> ordByEntry = allOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getMedicalEntry().getId()));
        Map<Long, List<OrderResultItem>> resultsByOrder = allOrders.isEmpty()
                ? Map.of()
                : orderResultRepository.findByMedicalOrderIdInOrderByIdAsc(
                        allOrders.stream().map(MedicalOrder::getId).toList())
                .stream().collect(Collectors.groupingBy(r -> r.getMedicalOrder().getId(),
                        Collectors.mapping(this::mapResult, Collectors.toList())));

        Map<Long, List<MedicalCertificate>> certByEntry = certificateRepository.findByMedicalEntryIdInOrderByIdAsc(entryIds)
                .stream().collect(Collectors.groupingBy(c -> c.getMedicalEntry().getId()));

        List<Long> userIds = new ArrayList<>();
        entries.forEach(e -> {
            if (e.getCreatedBy() != null) userIds.add(e.getCreatedBy());
            if (e.getSignedBy() != null) userIds.add(e.getSignedBy());
        });
        Map<Long, String> authorNames = resolveAuthorNames(userIds.stream().distinct().toList(), tenantId);

        return entries.stream()
                .map(e -> mapEntry(e,
                        presByEntry.getOrDefault(e.getId(), List.of()),
                        diagByEntry.getOrDefault(e.getId(), List.of()),
                        cie10Codes,
                        procByEntry.getOrDefault(e.getId(), List.of()),
                        ordByEntry.getOrDefault(e.getId(), List.of()),
                        resultsByOrder,
                        certByEntry.getOrDefault(e.getId(), List.of()),
                        authorNames))
                .toList();
    }

    private MedicalEntryResponse mapEntry(MedicalEntry e,
                                          List<Prescription> prescriptions,
                                          List<MedicalEntryDiagnosis> diagnoses,
                                          Map<Long, String> cie10Codes,
                                          List<MedicalProcedure> procedures,
                                          List<MedicalOrder> orders,
                                          Map<Long, List<OrderResultItem>> resultsByOrder,
                                          List<MedicalCertificate> certificates,
                                          Map<Long, String> authorNames) {

        List<PrescriptionResponse> pres = prescriptions.stream()
                .map(p -> new PrescriptionResponse(
                        p.getId(), p.getMedication(), p.getDosage(), p.getFrequency(), p.getDuration(),
                        p.getRoute(), p.getQuantity(), p.getPresentation(), p.getIsActive(), p.getInstructions()))
                .toList();

        List<DiagnosisItem> diag = diagnoses.stream()
                .map(d -> new DiagnosisItem(d.getId(), d.getCie10Id(),
                        d.getCie10Id() != null ? cie10Codes.get(d.getCie10Id()) : null,
                        d.getDescription(), d.getDiagnosisType(), d.getDiagnosisRank(), d.getNotes()))
                .toList();

        List<ProcedureItem> proc = procedures.stream()
                .map(p -> new ProcedureItem(p.getId(), p.getCode(), p.getName(), p.getNotes(), p.getPerformedAt()))
                .toList();

        List<OrderItem> ord = orders.stream()
                .map(o -> new OrderItem(o.getId(), o.getOrderType(), o.getDescription(), o.getStatus(),
                        o.getRequestedAt(), resultsByOrder.getOrDefault(o.getId(), List.of())))
                .toList();

        List<CertificateItem> cert = certificates.stream()
                .map(c -> new CertificateItem(c.getId(), c.getCertificateType(), c.getContent(),
                        c.getRestDays(), c.getIssuedAt(), c.getValidUntil()))
                .toList();

        return new MedicalEntryResponse(
                e.getId(),
                e.getAppointment() != null ? e.getAppointment().getId() : null,
                e.getEntryType(),
                e.getChiefComplaint(),
                e.getPresentIllness(),
                e.getReviewOfSystems(),
                e.getPhysicalExamination(),
                e.getAssessment(),
                e.getPlan(),
                e.getDiagnosis(),
                e.getTreatment(),
                e.getNotes(),
                e.getFollowUpAt(),
                e.getIsLocked(),
                e.getSignedBy(),
                e.getSignedBy() != null ? authorNames.get(e.getSignedBy()) : null,
                e.getSignedAt(),
                e.getCreatedAt(),
                e.getCreatedBy(),
                e.getCreatedBy() != null ? authorNames.get(e.getCreatedBy()) : null,
                pres, diag, proc, ord, cert
        );
    }

    private OrderResultItem mapResult(MedicalOrderResult r) {
        return new OrderResultItem(r.getId(), r.getResult(), r.getFileUrl(), r.getResultDate());
    }

    private Map<Long, String> resolveCie10Codes(List<MedicalEntryDiagnosis> diagnoses) {
        List<Long> ids = diagnoses.stream()
                .map(MedicalEntryDiagnosis::getCie10Id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        return cie10CodeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Cie10Code::getId, Cie10Code::getCode));
    }

    private Map<Long, String> resolveAuthorNames(List<Long> userIds, Long tenantId) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        return userIds.stream()
                .map(id -> userRepository.findByIdAndTenantId(id, tenantId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        User::getId,
                        u -> {
                            Person p = u.getPerson();
                            if (p == null) return u.getEmail();
                            return ((p.getFirstName() != null ? p.getFirstName() : "") + " "
                                    + (p.getLastName() != null ? p.getLastName() : "")).trim();
                        },
                        (a, b) -> a));
    }
}
