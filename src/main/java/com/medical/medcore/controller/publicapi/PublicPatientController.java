package com.medical.medcore.controller.publicapi;

import com.medical.medcore.dto.request.QuickPatientRegistrationRequest;
import com.medical.medcore.dto.response.EmailAvailabilityResponse;
import com.medical.medcore.dto.response.PatientResponse;
import com.medical.medcore.service.patient.PublicPatientService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/patients")
@RequiredArgsConstructor
public class PublicPatientController {

    private final PublicPatientService publicPatientService;

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkEmail(@RequestParam String email) {
        boolean available = publicPatientService.isEmailAvailable(email);
        return ResponseEntity.ok(
                new ApiResponse<>(true, new EmailAvailabilityResponse(available), null)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PatientResponse>> registerQuickPatient(
            @Valid @RequestBody QuickPatientRegistrationRequest request) {

        return ResponseEntity.ok(
                new ApiResponse<>(true, publicPatientService.registerQuickPatient(request), "Paciente registrado exitosamente")
        );
    }
}