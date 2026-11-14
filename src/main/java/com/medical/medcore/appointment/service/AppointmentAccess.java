package com.medical.medcore.appointment.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.doctor.repository.DoctorRepository;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.repository.PatientRepository;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.BranchScope;
import com.medical.medcore.shared.security.SecurityUtils;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AppointmentAccess {

    private static final String DENIED = "No tienes acceso a esta cita";

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public void assertCanAccess(Appointment appointment) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (SecurityUtils.isBranchScoped()) {
            assertBranch(appointment);
            return;
        }
        if (SecurityUtils.isDoctor()) {
            assertOwnDoctor(appointment);
            return;
        }
        assertOwnPatient(appointment);
    }

    public Patient currentPatient() {
        User user = currentUser();
        return patientRepository.findByPersonIdAndTenantId(user.getPerson().getId(), user.getTenantId())
                .orElseThrow(() -> new ForbiddenException("NO_PATIENT_PROFILE",
                        "No tienes un paciente asociado para esta operación"));
    }

    public Optional<Doctor> currentDoctor() {
        User user = currentUser();
        return doctorRepository.findByPersonIdAndTenantId(user.getPerson().getId(), user.getTenantId());
    }

    public Long restrictDoctorFilter(Long requestedDoctorId) {
        if (!SecurityUtils.isDoctorOnly()) {
            return requestedDoctorId;
        }
        Long ownDoctorId = currentDoctor()
                .map(Doctor::getId)
                .orElseThrow(() -> new ForbiddenException("NO_DOCTOR_PROFILE", "Tu usuario no tiene un perfil de médico"));
        if (requestedDoctorId != null && !requestedDoctorId.equals(ownDoctorId)) {
            throw new ForbiddenException("Solo puedes consultar tu propia agenda");
        }
        return ownDoctorId;
    }

    private void assertBranch(Appointment appointment) {
        Long branchId = appointment.getBranch() != null ? appointment.getBranch().getId() : null;
        try {
            BranchScope.assertAccess(branchId);
        } catch (ForbiddenException denied) {
            throw new ForbiddenException(DENIED);
        }
    }

    private void assertOwnDoctor(Appointment appointment) {
        Long ownDoctorId = currentDoctor().map(Doctor::getId).orElse(null);
        if (ownDoctorId == null || !ownDoctorId.equals(appointment.getDoctor().getId())) {
            throw new ForbiddenException(DENIED);
        }
    }

    private void assertOwnPatient(Appointment appointment) {
        if (!currentPatient().getId().equals(appointment.getPatient().getId())) {
            throw new ForbiddenException(DENIED);
        }
    }

    private User currentUser() {
        return userRepository.findByIdAndTenantId(TenantContext.requireCurrentUserId(), TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}
