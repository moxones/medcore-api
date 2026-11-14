package com.medical.medcore.doctor.service;

import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.doctor.dto.DoctorRequest;
import com.medical.medcore.doctor.dto.DoctorResponse;
import com.medical.medcore.doctor.dto.DoctorSelfResponse;
import com.medical.medcore.doctor.repository.DoctorRepository;
import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.repository.PersonRepository;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.RoleConstants;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private static final String NOT_FOUND = "Médico no encontrado";

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;

    public Doctor findMe() {
        Long tenantId = TenantContext.requireTenantId();
        User user = userRepository.findByIdAndTenantId(TenantContext.requireCurrentUserId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return doctorRepository.findByPersonIdAndTenantId(user.getPerson().getId(), tenantId)
                .orElseThrow(() -> new NotFoundException("NO_DOCTOR_PROFILE", "No existe un médico asociado a este usuario"));
    }

    public DoctorSelfResponse getMyProfile() {
        return DoctorSelfResponse.from(findMe());
    }

    public DoctorResponse findById(Long id) {
        return DoctorResponse.from(load(id));
    }

    @Transactional
    public DoctorResponse create(DoctorRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Long personId = request.resolvedPersonId();
        if (personId == null) {
            throw new BadRequestException("La persona del médico es obligatoria");
        }
        Person person = personRepository.findByIdAndTenantId(personId, tenantId)
                .orElseThrow(() -> new NotFoundException("Persona no encontrada"));
        if (doctorRepository.findByPersonIdAndTenantId(personId, tenantId).isPresent()) {
            throw new ConflictException("DOCTOR_ALREADY_REGISTERED", "Esta persona ya está registrada como médico");
        }
        boolean hasDoctorAccount = userRepository.findByPersonIdAndTenantId(personId, tenantId)
                .map(user -> user.hasRole(RoleConstants.DOCTOR))
                .orElse(false);
        if (!hasDoctorAccount) {
            throw new BadRequestException("DOCTOR_ROLE_REQUIRED", "La persona debe tener un usuario con rol de médico");
        }
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .tenantId(tenantId)
                .person(person)
                .licenseNumber(requireLicense(request.licenseNumber()))
                .build());
        return DoctorResponse.from(doctor);
    }

    @Transactional
    public DoctorResponse update(Long id, DoctorRequest request) {
        Doctor doctor = load(id);
        if (request.licenseNumber() != null) {
            doctor.setLicenseNumber(requireLicense(request.licenseNumber()));
        }
        if (request.isActive() != null) {
            doctor.setIsActive(request.isActive());
        }
        return DoctorResponse.from(doctorRepository.save(doctor));
    }

    @Transactional
    public void deactivate(Long id) {
        Doctor doctor = load(id);
        doctor.setIsActive(false);
        doctorRepository.save(doctor);
    }

    private Doctor load(Long id) {
        return doctorRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    private String requireLicense(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new BadRequestException("La matrícula profesional es obligatoria");
        }
        return licenseNumber.trim();
    }
}
