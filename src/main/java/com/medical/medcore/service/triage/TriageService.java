package com.medical.medcore.service.triage;

import com.medical.medcore.dto.request.TriageRequest;
import com.medical.medcore.dto.response.TriageResponse;
import com.medical.medcore.dto.response.TriageSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface TriageService {

    TriageResponse create(TriageRequest request);

    List<TriageResponse> listByAppointment(Long appointmentId);

    TriageResponse latestByAppointment(Long appointmentId);

    TriageResponse findById(Long id);

    List<TriageSummaryResponse> getDaySummary(Long doctorId, LocalDate date);
}
