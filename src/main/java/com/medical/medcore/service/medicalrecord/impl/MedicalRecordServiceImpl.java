package com.medical.medcore.service.medicalrecord.impl;

import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.CreateMedicalEntryRequest;
import com.medical.medcore.dto.request.PrescriptionRequest;
import com.medical.medcore.dto.request.UpdatePatientClinicalRequest;
import com.medical.medcore.dto.response.MedicalEntryResponse;
import com.medical.medcore.dto.response.MedicalRecordResponse;
import com.medical.medcore.dto.response.PrescriptionResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.MedicalEntry;
import com.medical.medcore.entity.MedicalRecord;
import com.medical.medcore.entity.Patient;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.Prescription;
import com.medical.medcore.entity.User;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.MedicalEntryRepository;
import com.medical.medcore.repository.MedicalRecordRepository;
import com.medical.medcore.repository.PatientRepository;
import com.medical.medcore.repository.PrescriptionRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.service.medicalrecord.MedicalRecordService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalEntryRepository medicalEntryRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponse getByPatientId(Long patientId) {
        Long tenantId = TenantContext.requireTenantId();
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));
        return buildRecordResponse(patient, tenantId);
    }

    @Override
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

    @Override
    @Transactional
    public MedicalEntryResponse addEntry(CreateMedicalEntryRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getCurrentUserId();

        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        if (!appointment.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }

        Patient patient = appointment.getPatient();
        MedicalRecord record = getOrCreateRecord(patient, tenantId, userId);

        MedicalEntry entry = MedicalEntry.builder()
                .medicalRecord(record)
                .appointment(appointment)
                .diagnosis(request.diagnosis())
                .treatment(request.treatment())
                .notes(request.notes())
                .createdBy(userId)
                .build();
        entry = medicalEntryRepository.save(entry);

        List<Prescription> prescriptions = new ArrayList<>();
        if (request.prescriptions() != null) {
            for (PrescriptionRequest pr : request.prescriptions()) {
                if (pr == null || pr.medication() == null || pr.medication().isBlank()) continue;
                prescriptions.add(Prescription.builder()
                        .medicalEntry(entry)
                        .medication(pr.medication())
                        .dosage(pr.dosage())
                        .frequency(pr.frequency())
                        .duration(pr.duration())
                        .instructions(pr.instructions())
                        .build());
            }
            if (!prescriptions.isEmpty()) {
                prescriptions = prescriptionRepository.saveAll(prescriptions);
            }
        }

        String authorName = resolveAuthorName(userId, tenantId);
        return mapEntry(entry, prescriptions, authorName);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalEntryResponse getEntry(Long entryId) {
        Long tenantId = TenantContext.requireTenantId();
        MedicalEntry entry = medicalEntryRepository.findByIdAndTenantId(entryId, tenantId)
                .orElseThrow(() -> new NotFoundException("Entrada de historia clínica no encontrada"));

        List<Prescription> prescriptions = prescriptionRepository.findByMedicalEntryId(entry.getId());
        String authorName = resolveAuthorName(entry.getCreatedBy(), tenantId);
        return mapEntry(entry, prescriptions, authorName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalEntryResponse> getEntriesByAppointment(Long appointmentId) {
        Long tenantId = TenantContext.requireTenantId();
        List<MedicalEntry> entries = medicalEntryRepository.findByAppointmentIdAndTenantId(appointmentId, tenantId);
        return mapEntries(entries, tenantId);
    }

    @Override
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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private MedicalRecord getOrCreateRecord(Patient patient, Long tenantId, Long userId) {
        return medicalRecordRepository.findByPatientIdAndTenantId(patient.getId(), tenantId)
                .orElseGet(() -> {
                    MedicalRecord newRecord = MedicalRecord.builder()
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
        java.time.LocalDateTime recordCreatedAt = null;

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
                .stream()
                .collect(Collectors.groupingBy(p -> p.getMedicalEntry().getId()));

        Map<Long, String> authorNames = resolveAuthorNames(
                entries.stream().map(MedicalEntry::getCreatedBy).filter(java.util.Objects::nonNull).distinct().toList(),
                tenantId);

        return entries.stream()
                .map(e -> mapEntry(
                        e,
                        presByEntry.getOrDefault(e.getId(), Collections.emptyList()),
                        e.getCreatedBy() != null ? authorNames.get(e.getCreatedBy()) : null))
                .toList();
    }

    private MedicalEntryResponse mapEntry(MedicalEntry e, List<Prescription> prescriptions, String authorName) {
        List<PrescriptionResponse> pres = prescriptions.stream()
                .map(p -> new PrescriptionResponse(
                        p.getId(), p.getMedication(), p.getDosage(),
                        p.getFrequency(), p.getDuration(), p.getInstructions()))
                .toList();

        return new MedicalEntryResponse(
                e.getId(),
                e.getAppointment() != null ? e.getAppointment().getId() : null,
                e.getDiagnosis(),
                e.getTreatment(),
                e.getNotes(),
                e.getCreatedAt(),
                e.getCreatedBy(),
                authorName,
                pres
        );
    }

    private String resolveAuthorName(Long userId, Long tenantId) {
        if (userId == null) return null;
        return resolveAuthorNames(List.of(userId), tenantId).get(userId);
    }

    private Map<Long, String> resolveAuthorNames(List<Long> userIds, Long tenantId) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        return userIds.stream()
                .map(id -> userRepository.findByIdAndTenantId(id, tenantId).orElse(null))
                .filter(java.util.Objects::nonNull)
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
