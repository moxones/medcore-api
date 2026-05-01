package com.medical.medcore.service.appointment.impl;

import com.medical.medcore.dto.request.CancelAppointmentRequest;
import com.medical.medcore.dto.request.CreateAppointmentRequest;
import com.medical.medcore.dto.request.RescheduleAppointmentRequest;
import com.medical.medcore.dto.request.UpdateAppointmentFlowRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.dto.response.TimeSlotResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.Patient;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.service.appointment.AppointmentService;
import com.medical.medcore.types.PageableResponse;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    
    // Suponemos ID 1 para agendada y 4 para cancelada basado en una BD típica
    private static final Long STATUS_SCHEDULED = 1L;
    private static final Long STATUS_CANCELLED = 4L;

    @Override
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        Long tenantId = TenantContext.getTenantId();

        Patient patient = new Patient();
        patient.setId(request.patientId());

        Doctor doctor = new Doctor();
        doctor.setId(request.doctorId());

        Appointment appointment = Appointment.builder()
                .tenantId(tenantId)
                .patient(patient)
                .doctor(doctor)
                .scheduledAt(request.scheduledAt())
                .reason(request.reason())
                .statusId(STATUS_SCHEDULED)
                .appointmentTypeId(request.appointmentTypeId())
                .durationMinutes(30) // Podría venir del tipo de cita o request
                .flowStatus("WAITING")
                .build();

        appointment = appointmentRepository.save(appointment);
        return mapToResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<AppointmentResponse> findAll(int page, int size, Long doctorId, Long statusId, LocalDate date) {
        Long tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledAt").descending());

        LocalDateTime startDate = date != null ? date.atStartOfDay() : null;
        LocalDateTime endDate = date != null ? date.plusDays(1).atStartOfDay() : null;

        Page<Appointment> resultPage = appointmentRepository.findByFilters(
                tenantId, doctorId, statusId, startDate, endDate, pageable);

        Page<AppointmentResponse> responsePage = resultPage.map(this::mapToResponse);

        return PageableResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getCalendar(LocalDate startDate, LocalDate endDate, Long doctorId, Long branchId) {
        Long tenantId = TenantContext.getTenantId();
        
        List<Appointment> appointments = appointmentRepository.findForCalendar(
                tenantId, 
                startDate.atStartOfDay(), 
                endDate.plusDays(1).atStartOfDay(), 
                doctorId, 
                branchId);

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        Long tenantId = TenantContext.getTenantId();
        
        // 1. Obtener citas existentes ese día
        List<Appointment> existingAppointments = appointmentRepository.findByDoctorAndDate(
                tenantId, doctorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay(), STATUS_CANCELLED);
                
        // 2. Extraer horas ocupadas
        List<LocalTime> occupiedTimes = existingAppointments.stream()
                .map(a -> a.getScheduledAt().toLocalTime())
                .toList();

        // 3. Generar slots (AQUÍ DEBERÍA CRUZARSE CON doctor_schedules)
        // Por simplicidad en este MVP generamos slots fijos de 08:00 a 18:00 cada 30 min.
        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime current = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(18, 0);
        
        while (current.isBefore(end)) {
            boolean isAvailable = !occupiedTimes.contains(current);
            slots.add(new TimeSlotResponse(current, current.plusMinutes(30), isAvailable));
            current = current.plusMinutes(30);
        }
        
        return slots;
    }

    @Override
    @Transactional
    public void reschedule(Long id, RescheduleAppointmentRequest request) {
        Appointment appointment = getOwnedAppointment(id);
        
        appointment.setScheduledAt(request.newScheduledAt());
        appointment.setReason(appointment.getReason() + " | Reschedule reason: " + request.reason());
        // Aquí normalmente crearíamos una entrada en appointment_reschedules
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void updateFlowStatus(Long id, UpdateAppointmentFlowRequest request) {
        Appointment appointment = getOwnedAppointment(id);
        
        appointment.setFlowStatus(request.flowStatus());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void cancel(Long id, CancelAppointmentRequest request) {
        Appointment appointment = getOwnedAppointment(id);
        
        appointment.setStatusId(STATUS_CANCELLED);
        appointment.setReason(appointment.getReason() + " | Cancellation reason: " + request.reason());
        appointmentRepository.save(appointment);
    }
    
    private Appointment getOwnedAppointment(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        
        if (!appointment.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to appointment");
        }
        return appointment;
    }

    private AppointmentResponse mapToResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                "Patient ID: " + (a.getPatient() != null ? a.getPatient().getId() : "N/A"),
                "Doctor ID: " + (a.getDoctor() != null ? a.getDoctor().getId() : "N/A"),
                "Branch ID: " + (a.getBranch() != null ? a.getBranch().getId() : "N/A"),
                a.getScheduledAt(),
                a.getStatusId(),
                a.getFlowStatus()
        );
    }
}
