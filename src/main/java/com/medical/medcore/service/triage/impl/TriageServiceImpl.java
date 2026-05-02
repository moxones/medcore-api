package com.medical.medcore.service.triage.impl;

import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.TriageRequest;
import com.medical.medcore.dto.response.TriageResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.Triage;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.TriageRepository;
import com.medical.medcore.service.triage.TriageService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TriageServiceImpl implements TriageService {

    private final TriageRepository triageRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public TriageResponse createOrUpdate(TriageRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));

        if (!appointment.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }

        Triage triage = triageRepository.findByAppointmentId(appointment.getId())
                .orElseGet(() -> {
                    Triage newTriage = new Triage();
                    newTriage.setAppointment(appointment);
                    return newTriage;
                });

        triage.setWeight(request.weight());
        triage.setHeight(request.height());
        triage.setTemperature(request.temperature());
        triage.setHeartRate(request.heartRate());
        triage.setBloodPressure(request.bloodPressure());
        triage.setNotes(request.notes());

        Long currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId != null) {
            triage.setCreatedBy(currentUserId);
        }

        triage = triageRepository.save(triage);

        appointment.setFlowStatus("TRIAGE_COMPLETED");
        appointmentRepository.save(appointment);

        return mapToResponse(triage);
    }

    @Override
    @Transactional(readOnly = true)
    public TriageResponse findByAppointment(Long appointmentId) {
        Long tenantId = TenantContext.requireTenantId();

        Triage triage = triageRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new NotFoundException("Triaje no encontrado"));

        if (!triage.getAppointment().getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a este triaje");
        }

        return mapToResponse(triage);
    }
    
    private TriageResponse mapToResponse(Triage t) {
        return new TriageResponse(
                t.getId(),
                t.getAppointment().getId(),
                t.getWeight(),
                t.getHeight(),
                t.getTemperature(),
                t.getHeartRate(),
                t.getBloodPressure(),
                t.getNotes()
        );
    }
}
