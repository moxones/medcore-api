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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
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

    public boolean isEmailAvailable(String email) {
        Long tenantId = TenantContext.requireTenantId();
        return userRepository.findByEmailAndTenantId(email, tenantId).isEmpty();
    }

    @Transactional
    public PatientResponse registerQuickPatient(QuickPatientRegistrationRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        log.info("=== REGISTER START ===");
        log.info("tenantId: {}, email: {}, documentType: {}, documentNumber: {}",
                tenantId, request.getEmail(), request.getDocumentTypeCode(), request.getDocumentNumber());

        tenantRepository.findByIdAndStatus(tenantId, TenantStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Tenant no encontrado o inactivo"));
        log.info("Tenant OK");

        if (userRepository.findByEmailAndTenantId(request.getEmail(), tenantId).isPresent()) {
            throw new BadRequestException("El email ya se encuentra registrado");
        }
        log.info("Email available OK");

        DocumentType docType = documentTypeRepository.findByCode(request.getDocumentTypeCode())
                .orElseThrow(() -> new BadRequestException("Tipo de documento inválido: " + request.getDocumentTypeCode()));
        log.info("DocumentType found: {}", docType.getCode());

        if (personDocumentRepository.findByDocumentTypeIdAndDocumentNumberAndTenantId(docType.getId(), request.getDocumentNumber(), tenantId).isPresent()) {
            throw new BadRequestException("El documento ya se encuentra registrado");
        }
        log.info("Document number available OK");

        Person person = Person.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .contactEmail(request.getEmail())
                .phone(request.getPhone())
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
        log.info("Role PATIENT found");

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .tenantId(tenantId)
                .person(person)
                .isActive(true)
                .roles(Set.of(rolePatient))
                .build();
        userRepository.save(user);
        log.info("User created successfully");

        return PatientResponse.builder()
                .id(patient.getId())
                .firstName(patient.getPerson().getFirstName())
                .lastName(patient.getPerson().getLastName())
                .contactEmail(patient.getPerson().getContactEmail())
                .build();
    }
}
