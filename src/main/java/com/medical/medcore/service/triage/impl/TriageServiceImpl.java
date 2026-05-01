package com.medical.medcore.service.triage.impl;

import com.medical.medcore.dto.request.TriageRequest;
import com.medical.medcore.dto.response.TriageResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.Triage;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.TriageRepository;
import com.medical.medcore.service.triage.TriageService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
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
        Long tenantId = TenantContext.getTenantId();
        
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
                
        if (!appointment.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized");
        }

        Triage triage = triageRepository.findByAppointmentId(appointment.getId())
                .orElse(new Triage());

        triage.setAppointment(appointment);
        triage.setWeight(request.weight());
        triage.setHeight(request.height());
        triage.setTemperature(request.temperature());
        triage.setHeartRate(request.heartRate());
        triage.setBloodPressure(request.bloodPressure());
        triage.setNotes(request.notes());

        triage = triageRepository.save(triage);
        
        // El frontend probablemente querrá que el flow_status cambie
        appointment.setFlowStatus("TRIAGE_COMPLETED");
        appointmentRepository.save(appointment);

        return mapToResponse(triage);
    }

    @Override
    @Transactional(readOnly = true)
    public TriageResponse findByAppointment(Long appointmentId) {
        Long tenantId = TenantContext.getTenantId();
        
        Triage triage = triageRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Triage not found"));
                
        if (!triage.getAppointment().getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized");
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
