package com.medical.medcore.service.patient.impl;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.CreatePatientRequest;
import com.medical.medcore.dto.request.UpdateProfileRequest;
import com.medical.medcore.dto.response.PatientResponse;
import com.medical.medcore.entity.*;
import com.medical.medcore.repository.*;
import com.medical.medcore.service.patient.PatientService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final PersonDocumentRepository personDocumentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final PatientSearchRepository patientSearchRepository;


@Override
    public PatientResponse create(CreatePatientRequest request) {

        Long tenantId = TenantContext.requireTenantId();

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

        return mapToResponse(patient);
    }

    @Override
    public List<PatientResponse> findAll() {

        Long tenantId = TenantContext.requireTenantId();

        return patientRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PatientResponse findById(Long id) {

        Long tenantId = TenantContext.requireTenantId();

        Patient patient = patientRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));

        return mapToResponse(patient);
    }

    @Override
    public void updateProfile(UpdateProfileRequest request) {

        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Person person = user.getPerson();

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
    public List<PatientResponse> search(String term) {

        Long tenantId = TenantContext.requireTenantId();

        List<Patient> patients;

        if (term.matches("\\d+")) {
            patients = patientSearchRepository.searchByDocument(tenantId, term);
        } else {
            patients = patientSearchRepository.searchByText(tenantId, term);
        }

        return patients.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PatientResponse mapToResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .firstName(patient.getPerson().getFirstName())
                .lastName(patient.getPerson().getLastName())
                .contactEmail(patient.getPerson().getContactEmail())
                .build();
    }
}
