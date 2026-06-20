package com.medical.medcore.controller;

import com.medical.medcore.dto.request.TriageRequest;
import com.medical.medcore.dto.response.TriageResponse;
import com.medical.medcore.dto.response.TriageSummaryResponse;
import com.medical.medcore.security.authorization.annotation.RequireStaff;
import com.medical.medcore.service.triage.TriageService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/triage")
@RequiredArgsConstructor
@RequireStaff
public class TriageController {

    private final TriageService triageService;

    @PostMapping
    public ResponseEntity<ApiResponse<TriageResponse>> create(
            @Valid @RequestBody TriageRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, triageService.create(request), "Toma de triaje registrada")
        );
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<TriageSummaryResponse>>> today(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, triageService.getDaySummary(doctorId, date), "Triajes del día")
        );
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ApiResponse<List<TriageResponse>>> listByAppointment(
            @PathVariable Long appointmentId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, triageService.listByAppointment(appointmentId), "Tomas de triaje de la cita")
        );
    }

    @GetMapping("/appointment/{appointmentId}/latest")
    public ResponseEntity<ApiResponse<TriageResponse>> latestByAppointment(
            @PathVariable Long appointmentId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, triageService.latestByAppointment(appointmentId), "Última toma de triaje")
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TriageResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, triageService.findById(id), "Toma de triaje")
        );
    }
}
