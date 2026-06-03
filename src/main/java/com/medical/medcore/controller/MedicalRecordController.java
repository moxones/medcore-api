package com.medical.medcore.controller;

import com.medical.medcore.dto.request.CreateMedicalEntryRequest;
import com.medical.medcore.dto.request.UpdatePatientClinicalRequest;
import com.medical.medcore.dto.response.MedicalEntryResponse;
import com.medical.medcore.dto.response.MedicalRecordResponse;
import com.medical.medcore.security.authorization.annotation.RequireDoctorOrAdmin;
import com.medical.medcore.security.authorization.annotation.RequireStaff;
import com.medical.medcore.service.medicalrecord.MedicalRecordService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @RequireStaff
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> findByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.getByPatientId(patientId), "Historia clínica"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> myRecord() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.getMyRecord(), "Mi historia clínica"));
    }

    @RequireDoctorOrAdmin
    @PostMapping("/entries")
    public ResponseEntity<ApiResponse<MedicalEntryResponse>> addEntry(
            @Valid @RequestBody CreateMedicalEntryRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.addEntry(request), "Atención registrada en la historia clínica"));
    }

    @RequireStaff
    @GetMapping("/entries/{entryId}")
    public ResponseEntity<ApiResponse<MedicalEntryResponse>> getEntry(@PathVariable Long entryId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.getEntry(entryId), "Entrada de historia clínica"));
    }

    @RequireStaff
    @GetMapping("/appointment/{appointmentId}/entries")
    public ResponseEntity<ApiResponse<List<MedicalEntryResponse>>> getByAppointment(
            @PathVariable Long appointmentId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.getEntriesByAppointment(appointmentId), "Atenciones de la cita"));
    }

    @RequireDoctorOrAdmin
    @PutMapping("/patient/{patientId}/clinical")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> updateClinical(
            @PathVariable Long patientId,
            @RequestBody UpdatePatientClinicalRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.updatePatientClinical(patientId, request), "Base clínica actualizada"));
    }
}
