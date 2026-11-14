package com.medical.medcore.triage.controller;

import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireClinicalStaff;
import com.medical.medcore.triage.dto.TriageRequest;
import com.medical.medcore.triage.dto.TriageResponse;
import com.medical.medcore.triage.dto.TriageSummaryResponse;
import com.medical.medcore.triage.service.TriageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/triage")
@RequiredArgsConstructor
@RequireClinicalStaff
public class TriageController {

    private final TriageService triageService;

    @PostMapping
    public ApiResponse<TriageResponse> create(@Valid @RequestBody TriageRequest request) {
        return ApiResponse.ok(triageService.create(request), "Toma de triaje registrada");
    }

    @GetMapping("/today")
    public ApiResponse<List<TriageSummaryResponse>> today(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(triageService.getDaySummary(branchId, doctorId, date, urgency), "Triajes del día");
    }

    @GetMapping("/patient/{patientId}")
    public ApiResponse<List<TriageSummaryResponse>> listByPatient(@PathVariable Long patientId) {
        return ApiResponse.ok(triageService.listByPatient(patientId), "Triajes del paciente");
    }

    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<List<TriageResponse>> listByAppointment(@PathVariable Long appointmentId) {
        return ApiResponse.ok(triageService.listByAppointment(appointmentId), "Tomas de triaje de la cita");
    }

    @GetMapping("/appointment/{appointmentId}/latest")
    public ApiResponse<TriageResponse> latestByAppointment(@PathVariable Long appointmentId) {
        return ApiResponse.ok(triageService.latestByAppointment(appointmentId), "Última toma de triaje");
    }

    @GetMapping("/{id}")
    public ApiResponse<TriageResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(triageService.findById(id), "Toma de triaje");
    }
}
