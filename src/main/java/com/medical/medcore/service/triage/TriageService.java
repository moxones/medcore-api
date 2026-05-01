package com.medical.medcore.service.triage;

import com.medical.medcore.dto.request.TriageRequest;
import com.medical.medcore.dto.response.TriageResponse;

public interface TriageService {
    TriageResponse createOrUpdate(TriageRequest request);
    TriageResponse findByAppointment(Long appointmentId);
}
