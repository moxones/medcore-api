package com.medical.medcore.patient.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.identity.service.PersonData;
import com.medical.medcore.identity.service.PersonRegistry;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.dto.CreatePatientRequest;
import com.medical.medcore.patient.dto.PatientProfileResponse;
import com.medical.medcore.patient.dto.PatientResponse;
import com.medical.medcore.patient.dto.UpdatePatientRequest;
import com.medical.medcore.patient.dto.UpdateProfileRequest;
import com.medical.medcore.patient.repository.PatientRepository;
import com.medical.medcore.shared.api.PageableResponse;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_RESULTS = 50;
    private static final int MIN_SEARCH_LENGTH = 2;
    private static final String PATIENT_NOT_FOUND = "Paciente no encontrado";

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PersonRegistry personRegistry;
    private final PatientEnrollment patientEnrollment;

    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        PersonData data = new PersonData(
                request.firstName(), request.lastName(), request.birthDate(), request.gender(), request.phone(),
                request.contactEmail(), request.address(), request.district(),
                request.documentTypeCode(), request.documentNumber());

        Person person = personRegistry.findByDocument(tenantId, data.documentTypeCode(), data.documentNumber())
                .map(existing -> assertNotYetPatient(existing, tenantId))
                .orElseGet(() -> personRegistry.create(tenantId, data));

        Patient patient = patientEnrollment.ensurePatient(tenantId, person);
        patient.setEmergencyContactName(clean(request.emergencyContactName()));
        patient.setEmergencyContactPhone(clean(request.emergencyContactPhone()));
        patientRepository.save(patient);
        return PatientMapper.toResponse(patient, accountOf(person, tenantId));
    }

    public PageableResponse<PatientResponse> findAll(int page, int size) {
        Long tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by("id").descending());
        Page<Patient> result = patientRepository.findByTenantIdWithPerson(tenantId, pageable);
        Map<Long, User> accounts = accountsByPerson(result.getContent(), tenantId);
        return PageableResponse.from(result.map(
                patient -> PatientMapper.toResponse(patient, accounts.get(patient.getPerson().getId()))));
    }

    public PatientResponse findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Patient patient = load(id, tenantId);
        return PatientMapper.toResponse(patient, accountOf(patient.getPerson(), tenantId));
    }

    @Transactional
    public PatientResponse update(Long id, UpdatePatientRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Patient patient = load(id, tenantId);
        personRegistry.applyChanges(patient.getPerson(), new PersonData(
                request.firstName(), request.lastName(), request.birthDate(), request.gender(), request.phone(),
                request.contactEmail(), request.address(), request.district(),
                request.documentTypeCode(), request.documentNumber()));
        if (request.emergencyContactName() != null) {
            patient.setEmergencyContactName(clean(request.emergencyContactName()));
        }
        if (request.emergencyContactPhone() != null) {
            patient.setEmergencyContactPhone(clean(request.emergencyContactPhone()));
        }
        if (request.isActive() != null) {
            patient.setIsActive(request.isActive());
        }
        patientRepository.save(patient);
        return PatientMapper.toResponse(patient, accountOf(patient.getPerson(), tenantId));
    }

    public List<PatientResponse> search(String rawTerm) {
        Long tenantId = TenantContext.requireTenantId();
        String term = rawTerm == null ? "" : rawTerm.trim();
        if (term.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }
        Pageable limit = PageRequest.of(0, MAX_SEARCH_RESULTS);
        String escaped = escapeLike(term.toLowerCase());
        List<Patient> patients = term.matches("\\d+")
                ? patientRepository.searchByDigits(tenantId, escaped + "%", "%" + escaped + "%", limit)
                : patientRepository.searchByText(tenantId, "%" + escaped + "%", limit);
        Map<Long, User> accounts = accountsByPerson(patients, tenantId);
        return patients.stream()
                .map(patient -> PatientMapper.toResponse(patient, accounts.get(patient.getPerson().getId())))
                .toList();
    }

    public PatientProfileResponse getProfile() {
        User account = currentAccount();
        Patient patient = patientRepository
                .findByPersonIdAndTenantId(account.getPerson().getId(), account.getTenantId())
                .orElse(null);
        return PatientMapper.toProfile(account, patient);
    }

    @Transactional
    public PatientProfileResponse updateProfile(UpdateProfileRequest request) {
        User account = currentAccount();
        personRegistry.applyChanges(account.getPerson(), new PersonData(
                request.firstName(), request.lastName(), request.birthDate(), request.gender(), request.phone(),
                request.contactEmail(), request.address(), request.district(), null, null));
        Patient patient = patientRepository
                .findByPersonIdAndTenantId(account.getPerson().getId(), account.getTenantId())
                .orElse(null);
        if (patient != null) {
            if (request.emergencyContactName() != null) {
                patient.setEmergencyContactName(clean(request.emergencyContactName()));
            }
            if (request.emergencyContactPhone() != null) {
                patient.setEmergencyContactPhone(clean(request.emergencyContactPhone()));
            }
            patientRepository.save(patient);
        }
        return PatientMapper.toProfile(account, patient);
    }

    private Person assertNotYetPatient(Person person, Long tenantId) {
        if (patientRepository.existsByPersonIdAndTenantId(person.getId(), tenantId)) {
            throw new ConflictException("PATIENT_ALREADY_EXISTS", "Ya existe un paciente registrado con ese documento");
        }
        return person;
    }

    private Patient load(Long id, Long tenantId) {
        return patientRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException(PATIENT_NOT_FOUND));
    }

    private User currentAccount() {
        return userRepository.findByIdAndTenantId(TenantContext.requireCurrentUserId(), TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private User accountOf(Person person, Long tenantId) {
        return userRepository.findByPersonIdAndTenantId(person.getId(), tenantId).orElse(null);
    }

    private Map<Long, User> accountsByPerson(List<Patient> patients, Long tenantId) {
        if (patients.isEmpty()) {
            return Map.of();
        }
        List<Long> personIds = patients.stream().map(patient -> patient.getPerson().getId()).toList();
        return userRepository.findByPersonIdInAndTenantId(personIds, tenantId).stream()
                .collect(Collectors.toMap(user -> user.getPerson().getId(), Function.identity(), (left, right) -> left));
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
