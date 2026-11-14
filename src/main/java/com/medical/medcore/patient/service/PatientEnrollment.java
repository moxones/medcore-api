package com.medical.medcore.patient.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.repository.PersonRepository;
import com.medical.medcore.identity.service.UserRolesAssignedEvent;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.repository.PatientRepository;
import com.medical.medcore.shared.security.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PatientEnrollment {

    private final PatientRepository patientRepository;
    private final PersonRepository personRepository;

    @Transactional
    public Patient ensurePatient(Long tenantId, Person person) {
        return patientRepository.findByPersonIdAndTenantId(person.getId(), tenantId)
                .orElseGet(() -> enroll(tenantId, person));
    }

    @EventListener
    @Transactional
    public void onRolesAssigned(UserRolesAssignedEvent event) {
        if (!event.roleCodes().contains(RoleConstants.PATIENT)) {
            return;
        }
        personRepository.findByIdAndTenantId(event.personId(), event.tenantId())
                .ifPresent(person -> ensurePatient(event.tenantId(), person));
    }

    private Patient enroll(Long tenantId, Person person) {
        Patient patient = patientRepository.save(Patient.builder()
                .tenantId(tenantId)
                .person(person)
                .build());
        patient.assignRecordNumber();
        return patientRepository.save(patient);
    }
}
