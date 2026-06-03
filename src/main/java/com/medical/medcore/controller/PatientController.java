package com.medical.medcore.controller;

import com.medical.medcore.dto.request.CreatePatientRequest;
import com.medical.medcore.dto.request.UpdatePatientRequest;
import com.medical.medcore.dto.request.UpdateProfileRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.dto.response.PatientProfileResponse;
import com.medical.medcore.dto.response.PatientResponse;
import com.medical.medcore.security.authorization.annotation.RequireStaff;
import com.medical.medcore.service.patient.PatientService;
import com.medical.medcore.types.ApiResponse;
import com.medical.medcore.types.PageableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @RequireStaff
    @PostMapping
    public PatientResponse create(@RequestBody CreatePatientRequest request) {
        return patientService.create(request);
    }

    @RequireStaff
    @GetMapping
    public ResponseEntity<ApiResponse<PageableResponse<PatientResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, patientService.findAll(page, size), "Lista de pacientes")
        );
    }

    @RequireStaff
    @GetMapping("/{id}")
    public PatientResponse findById(@PathVariable Long id) {
        return patientService.findById(id);
    }

    @RequireStaff
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable Long id,
            @RequestBody UpdatePatientRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, patientService.updatePatient(id, request), "Paciente actualizado")
        );
    }

    @GetMapping("/profile")
    public PatientProfileResponse getProfile() {
        return patientService.getProfile();
    }

    @PutMapping("/profile")
    public void updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        patientService.updateProfile(request);
    }

    @RequireStaff
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> search(@RequestParam String query) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, patientService.search(query), "Pacientes encontrados")
        );
    }

    @GetMapping("/me/appointments")
    public ResponseEntity<ApiResponse<PageableResponse<AppointmentResponse>>> getMyAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String flowStatus) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, patientService.getMyAppointments(page, size, statusId, date, flowStatus), "Mis citas")
        );
    }
}