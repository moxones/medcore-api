package com.medical.medcore.service.patient.impl;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.CreatePatientRequest;
import com.medical.medcore.dto.request.UpdatePatientRequest;
import com.medical.medcore.dto.request.UpdateProfileRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.dto.response.PatientProfileResponse;
import com.medical.medcore.dto.response.PatientResponse;
import com.medical.medcore.entity.*;
import com.medical.medcore.repository.*;
import com.medical.medcore.service.appointment.impl.AppointmentServiceImpl;
import com.medical.medcore.service.patient.PatientService;
import com.medical.medcore.types.PageableResponse;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final PersonDocumentRepository personDocumentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final PatientSearchRepository patientSearchRepository;
    private final AppointmentRepository appointmentRepository;


@Override
    public PatientResponse create(CreatePatientRequest request) {

        Long tenantId = TenantContext.requireTenantId();

        if (request.getFirstName() == null || request.getFirstName().isBlank()
                || request.getLastName() == null || request.getLastName().isBlank()) {
            throw new BadRequestException("El nombre y apellido son obligatorios");
        }

        DocumentType docType = documentTypeRepository.findByCode(request.getDocumentTypeCode())
                .orElseThrow(() -> new BadRequestException("Tipo de documento inválido"));

        PersonDocument existingDoc = personDocumentRepository
                .findByDocumentTypeIdAndDocumentNumberAndTenantId(docType.getId(), request.getDocumentNumber(), tenantId)
                .orElse(null);

        Person person;

        if (existingDoc != null) {
            person = existingDoc.getPerson();

            if (!person.getTenantId().equals(tenantId)) {
                throw new BadRequestException("Documento ya existe en otro tenant");
            }
        } else {
            person = Person.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .birthDate(request.getBirthDate())
                    .tenantId(tenantId)
                    .build();

            person.recalculateProfileCompleted(true);
            person = personRepository.save(person);

            PersonDocument document = PersonDocument.builder()
                    .person(person)
                    .documentType(docType)
                    .documentNumber(request.getDocumentNumber())
                    .build();

            personDocumentRepository.save(document);
        }

        Optional<Patient> existingPatient =
                patientRepository.findByPersonIdAndTenantId(person.getId(), tenantId);

        if (existingPatient.isPresent()) {
            throw new BadRequestException("Paciente ya existe en este tenant");
        }

        Patient patient = Patient.builder()
                .tenantId(tenantId)
                .person(person)
                .build();

        patient = patientRepository.save(patient);

        User user = userRepository.findByPersonIdAndTenantId(person.getId(), tenantId).orElse(null);
        return mapToResponse(patient, user);
    }

    @Override
    public PageableResponse<PatientResponse> findAll(int page, int size) {

        Long tenantId = TenantContext.requireTenantId();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Patient> resultPage = patientRepository.findByTenantIdWithPerson(tenantId, pageable);

        List<Long> personIds = resultPage.getContent().stream()
                .map(p -> p.getPerson().getId())
                .toList();

        Map<Long, User> usersByPersonId = userRepository.findByPersonIdInAndTenantId(personIds, tenantId)
                .stream()
                .collect(Collectors.toMap(u -> u.getPerson().getId(), u -> u));

        return PageableResponse.from(resultPage.map(p -> mapToResponse(p, usersByPersonId.get(p.getPerson().getId()))));
    }

    @Override
    public PatientResponse findById(Long id) {

        Long tenantId = TenantContext.requireTenantId();

        Patient patient = patientRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));

        User user = userRepository.findByPersonIdAndTenantId(patient.getPerson().getId(), tenantId).orElse(null);
        return mapToResponse(patient, user);
    }

    @Override
    public PatientProfileResponse getProfile() {

        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Person person = user.getPerson();
        boolean hasDocument = personDocumentRepository.existsByPersonId(person.getId());

        Patient patient = patientRepository.findByPersonIdAndTenantId(person.getId(), tenantId).orElse(null);

        return PatientProfileResponse.builder()
                .id(person.getId())
                .patientId(patient != null ? patient.getId() : null)
                .firstName(person.getFirstName())
                .lastName(person.getLastName())
                .phone(person.getPhone())
                .gender(person.getGender())
                .birthDate(person.getBirthDate() != null ? person.getBirthDate().toString() : null)
                .contactEmail(person.getContactEmail())
                .profileCompleted(person.getProfileCompleted() != null ? person.getProfileCompleted() : false)
                .hasAccount(user != null)
                .accountActive(user != null ? user.getIsActive() : null)
                .build();
    }

    @Override
    public void updateProfile(UpdateProfileRequest request) {

        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Person person = user.getPerson();

        if (request.getFirstName() != null) {
            person.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            person.setLastName(request.getLastName());
        }

        if (request.getPhone() != null) {
            person.setPhone(request.getPhone());
        }

        if (request.getGender() != null) {
            person.setGender(request.getGender());
        }

        if (request.getBirthDate() != null) {
            person.setBirthDate(request.getBirthDate());
        }

        if (request.getContactEmail() != null) {
            String email = request.getContactEmail().trim();

            if (email.isEmpty()) {
                throw new BadRequestException("Email no puede ser vacío");
            }

            person.setContactEmail(email);
        }

        boolean hasDocument = personDocumentRepository.existsByPersonId(person.getId());
        person.recalculateProfileCompleted(hasDocument);

        personRepository.save(person);
    }

    @Override
    public PatientResponse updatePatient(Long id, UpdatePatientRequest request) {

        Long tenantId = TenantContext.requireTenantId();

        Patient patient = patientRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));

        Person person = patient.getPerson();

        if (request.getFirstName() != null) {
            person.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            person.setLastName(request.getLastName());
        }

        if (request.getPhone() != null) {
            person.setPhone(request.getPhone());
        }

        if (request.getGender() != null) {
            person.setGender(request.getGender());
        }

        if (request.getBirthDate() != null) {
            person.setBirthDate(request.getBirthDate());
        }

        if (request.getContactEmail() != null) {
            String email = request.getContactEmail().trim();
            if (!email.isEmpty()) {
                person.setContactEmail(email);
            }
        }

        boolean hasDocument = personDocumentRepository.existsByPersonId(person.getId());
        person.recalculateProfileCompleted(hasDocument);

        personRepository.save(person);

        User user = userRepository.findByPersonIdAndTenantId(person.getId(), tenantId).orElse(null);
        return mapToResponse(patient, user);
    }

    @Override
    public List<PatientResponse> search(String term) {

        Long tenantId = TenantContext.requireTenantId();

        List<Patient> patients;

        if (term.matches("\\d+")) {
            patients = patientSearchRepository.searchByDocument(tenantId, term);
        } else {
            patients = patientSearchRepository.searchByText(tenantId, term);
        }

        List<Long> personIds = patients.stream()
                .map(p -> p.getPerson().getId())
                .toList();

        Map<Long, User> usersByPersonId = userRepository.findByPersonIdInAndTenantId(personIds, tenantId)
                .stream()
                .collect(Collectors.toMap(u -> u.getPerson().getId(), u -> u));

        return patients.stream()
                .map(p -> mapToResponse(p, usersByPersonId.get(p.getPerson().getId())))
                .toList();
    }

    @Override
    public PageableResponse<AppointmentResponse> getMyAppointments(int page, int size, Long statusId, LocalDate date, String flowStatus) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Patient patient = patientRepository.findByPersonIdAndTenantId(user.getPerson().getId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado para este usuario"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledAt").descending());

        LocalDateTime startDate = date != null ? date.atStartOfDay() : null;
        LocalDateTime endDate = date != null ? date.plusDays(1).atStartOfDay() : null;

        Page<Appointment> resultPage = appointmentRepository.findByFilters(
                tenantId, null, patient.getId(), statusId, startDate, endDate, flowStatus,
                false, java.util.List.of(-1L), pageable);

        return PageableResponse.from(resultPage.map(this::mapAppointmentToResponse));
    }

    private AppointmentResponse mapAppointmentToResponse(Appointment a) {
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

    private PatientResponse mapToResponse(Patient patient, User user) {
        Person p = patient.getPerson();
        return PatientResponse.builder()
                .id(patient.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .phone(p.getPhone())
                .gender(p.getGender())
                .birthDate(p.getBirthDate() != null ? p.getBirthDate().toString() : null)
                .contactEmail(p.getContactEmail())
                .profileCompleted(p.getProfileCompleted())
                .hasAccount(user != null)
                .userEmail(user != null ? user.getEmail() : null)
                .accountActive(user != null ? user.getIsActive() : null)
                .build();
    }
}
