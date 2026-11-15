package com.medical.medcore.patient.controller;

import com.medical.medcore.identity.controller.RefreshTokenTransport;
import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.patient.dto.EmailAvailabilityResponse;
import com.medical.medcore.patient.dto.PatientResponse;
import com.medical.medcore.patient.dto.PatientSelfRegistrationRequest;
import com.medical.medcore.patient.dto.QuickPatientRegistrationRequest;
import com.medical.medcore.patient.service.PatientRegistrationService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.shared.security.annotation.RequireModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequireModule(ClinicModule.PATIENT_PORTAL)
public class PublicPatientController {

    private final PatientRegistrationService registrationService;
    private final RefreshTokenTransport refreshTokenTransport;

    @GetMapping("/public/patients/check-email")
    public ApiResponse<EmailAvailabilityResponse> checkEmail(@RequestParam String email) {
        return ApiResponse.ok(new EmailAvailabilityResponse(registrationService.isEmailAvailable(email)));
    }

    @PostMapping("/public/patients/register")
    public ApiResponse<PatientResponse> registerQuick(@Valid @RequestBody QuickPatientRegistrationRequest request) {
        return ApiResponse.ok(registrationService.registerQuick(request), "Paciente registrado exitosamente");
    }

    @PostMapping("/auth/register")
    public ApiResponse<AuthResponse> registerSelf(@Valid @RequestBody PatientSelfRegistrationRequest request,
                                                  HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthResponse session = registrationService.registerSelf(request);
        return ApiResponse.ok(refreshTokenTransport.deliver(session, httpRequest, httpResponse), "Cuenta creada");
    }
}
