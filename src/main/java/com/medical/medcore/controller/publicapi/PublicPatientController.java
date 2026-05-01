package com.medical.medcore.controller.publicapi;

import com.medical.medcore.dto.request.QuickPatientRegistrationRequest;
import com.medical.medcore.dto.response.PatientResponse;
import com.medical.medcore.service.patient.PublicPatientService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/patients")
@RequiredArgsConstructor
public class PublicPatientController {

    private final PublicPatientService publicPatientService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PatientResponse>> registerQuickPatient(
            @Valid @RequestBody QuickPatientRegistrationRequest request) {
        
        return ResponseEntity.ok(
                new ApiResponse<>(true, publicPatientService.registerQuickPatient(request), "Paciente registrado exitosamente")
        );
    }
}
