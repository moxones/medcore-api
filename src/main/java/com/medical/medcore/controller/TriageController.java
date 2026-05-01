package com.medical.medcore.controller;

import com.medical.medcore.dto.request.TriageRequest;
import com.medical.medcore.dto.response.TriageResponse;
import com.medical.medcore.service.triage.TriageService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    @PostMapping
    public ResponseEntity<ApiResponse<TriageResponse>> createOrUpdate(
            @Valid @RequestBody TriageRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, triageService.createOrUpdate(request), "Triage guardado exitosamente")
        );
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ApiResponse<TriageResponse>> findByAppointment(
            @PathVariable Long appointmentId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, triageService.findByAppointment(appointmentId), "Triage obtenido")
        );
    }
}
