package com.medical.medcore.patient.controller;

import com.medical.medcore.patient.dto.CreatePatientRequest;
import com.medical.medcore.patient.dto.PatientProfileResponse;
import com.medical.medcore.patient.dto.PatientResponse;
import com.medical.medcore.patient.dto.PortalInvitationRequest;
import com.medical.medcore.patient.dto.PortalInvitationResponse;
import com.medical.medcore.patient.dto.UpdatePatientRequest;
import com.medical.medcore.patient.dto.UpdateProfileRequest;
import com.medical.medcore.patient.service.PatientInvitationService;
import com.medical.medcore.patient.service.PatientService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.api.PageableResponse;
import com.medical.medcore.shared.security.annotation.RequireReception;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import com.medical.medcore.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.shared.security.annotation.RequireModule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final PatientInvitationService invitationService;

    @RequireStaff
    @PostMapping
    public ApiResponse<PatientResponse> create(@Valid @RequestBody CreatePatientRequest request) {
        return ApiResponse.ok(patientService.create(request), "Paciente registrado");
    }

    @RequireStaff
    @GetMapping
    public ApiResponse<PageableResponse<PatientResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(patientService.findAll(page, size), "Lista de pacientes");
    }

    @RequireStaff
    @GetMapping("/search")
    public ApiResponse<List<PatientResponse>> search(@RequestParam String query) {
        return ApiResponse.ok(patientService.search(query), "Pacientes encontrados");
    }

    @RequireModule(ClinicModule.PATIENT_PORTAL)
    @GetMapping("/profile")
    public ApiResponse<PatientProfileResponse> getProfile() {
        return ApiResponse.ok(patientService.getProfile());
    }

    @RequireModule(ClinicModule.PATIENT_PORTAL)
    @PutMapping("/profile")
    public ApiResponse<PatientProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(patientService.updateProfile(request), "Perfil actualizado");
    }

    @RequireStaff
    @GetMapping("/{id}")
    public ApiResponse<PatientResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(patientService.findById(id));
    }

    @RequireStaff
    @PutMapping("/{id}")
    public ApiResponse<PatientResponse> update(@PathVariable Long id, @Valid @RequestBody UpdatePatientRequest request) {
        return ApiResponse.ok(patientService.update(id, request), "Paciente actualizado");
    }

    @RequireReception
    @RequireModule(ClinicModule.PATIENT_PORTAL)
    @PostMapping("/{id}/portal-invitation")
    public ApiResponse<PortalInvitationResponse> invite(@PathVariable Long id,
                                                        @Valid @RequestBody(required = false) PortalInvitationRequest request,
                                                        HttpServletRequest httpRequest) {
        PortalInvitationResponse invitation = invitationService.invite(
                id, request != null ? request.email() : null, ClientIpResolver.resolve(httpRequest));
        String message = invitation.delivered()
                ? "Invitación enviada a " + invitation.email()
                : "No pudimos enviar el correo. Comparte el enlace de activación con el paciente";
        return ApiResponse.ok(invitation, message);
    }
}
