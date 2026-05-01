package com.medical.medcore.service.booking;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.dto.auth.RegisterRequest;
import com.medical.medcore.dto.request.PublicBookingRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.Patient;
import com.medical.medcore.entity.User;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.PatientRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.service.auth.AuthService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicBookingService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public AppointmentResponse registerAndBook(PublicBookingRequest request) {
        Long tenantId = TenantContext.getTenantId();
        
        // 1. Registrar al usuario si no existe
        User user = userRepository.findByEmailAndTenantId(request.email(), tenantId).orElse(null);
        
        if (user == null) {
            RegisterRequest registerReq = new RegisterRequest();
            registerReq.setFirstName(request.firstName());
            registerReq.setLastName(request.lastName());
            registerReq.setEmail(request.email());
            registerReq.setPassword(request.password());
            registerReq.setDocumentTypeCode(request.documentTypeCode());
            registerReq.setDocumentNumber(request.documentNumber());
            registerReq.setBirthDate(request.birthDate());
            // register_req.setPhone() si estuviera disponible en el base DTO
            
            authService.register(registerReq);
            user = userRepository.findByEmailAndTenantId(request.email(), tenantId)
                    .orElseThrow(() -> new RuntimeException("Error creando usuario"));
        }

        Patient patient = patientRepository.findByPersonIdAndTenantId(user.getPerson().getId(), tenantId)
                .orElseThrow(() -> new BadRequestException("El usuario no tiene perfil de paciente"));

        // 2. Crear la cita
        Doctor doctor = new Doctor();
        doctor.setId(request.doctorId());

        Appointment appointment = Appointment.builder()
                .tenantId(tenantId)
                .patient(patient)
                .doctor(doctor)
                .scheduledAt(request.scheduledAt())
                .reason(request.reason())
                .statusId(1L) // SCHEDULED
                .appointmentTypeId(request.appointmentTypeId())
                .durationMinutes(30)
                .flowStatus("WAITING")
                .createdBy(user.getId()) // MAPEO IDEAL: Indica que fue creado por sí mismo
                .build();

        appointment = appointmentRepository.save(appointment);

        return new AppointmentResponse(
                appointment.getId(),
                patient.getPerson().getFirstName() + " " + patient.getPerson().getLastName(),
                "Doctor " + doctor.getId(),
                "Branch " + request.branchId(),
                appointment.getScheduledAt(),
                appointment.getStatusId(),
                appointment.getFlowStatus()
        );
    }
}
