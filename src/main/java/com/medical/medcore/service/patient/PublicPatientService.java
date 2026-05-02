package com.medical.medcore.service.patient;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.QuickPatientRegistrationRequest;
import com.medical.medcore.dto.response.PatientResponse;
import com.medical.medcore.entity.*;
import com.medical.medcore.entity.enums.TenantStatus;
import com.medical.medcore.repository.*;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class PublicPatientService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final PersonDocumentRepository personDocumentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final PatientRepository patientRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PatientResponse registerQuickPatient(QuickPatientRegistrationRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        tenantRepository.findByIdAndStatus(tenantId, TenantStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Tenant no encontrado o inactivo"));

        if (userRepository.findByEmailAndTenantId(request.getEmail(), tenantId).isPresent()) {
            throw new BadRequestException("El email ya se encuentra registrado");
        }

        DocumentType docType = documentTypeRepository.findByCode(request.getDocumentTypeCode())
                .orElseThrow(() -> new BadRequestException("Tipo de documento inválido"));

        if (personDocumentRepository.findByDocumentTypeIdAndDocumentNumberAndTenantId(docType.getId(), request.getDocumentNumber(), tenantId).isPresent()) {
            throw new BadRequestException("El documento ya se encuentra registrado");
        }

        Person person = Person.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .contactEmail(request.getEmail())
                .tenantId(tenantId)
                .build();

        // Use the existing field and set to false by default for this flow
        person.recalculateProfileCompleted(false);
        person = personRepository.save(person);

        PersonDocument document = PersonDocument.builder()
                .person(person)
                .documentType(docType)
                .documentNumber(request.getDocumentNumber())
                .build();
        personDocumentRepository.save(document);

        Patient patient = Patient.builder()
                .tenantId(tenantId)
                .person(person)
                .build();
        patient = patientRepository.save(patient);

        Role rolePatient = roleRepository.findByCode("PATIENT")
                .orElseThrow(() -> new BadRequestException("Rol PATIENT no configurado"));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .tenantId(tenantId)
                .person(person)
                .isActive(true)
                .roles(Set.of(rolePatient))
                .build();
        userRepository.save(user);

        return PatientResponse.builder()
                .id(patient.getId())
                .firstName(patient.getPerson().getFirstName())
                .lastName(patient.getPerson().getLastName())
                .contactEmail(patient.getPerson().getContactEmail())
                .build();
    }
}
