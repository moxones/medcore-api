package com.medical.medcore.clinical.controller;

import com.medical.medcore.clinical.dto.CreateMedicalEntryRequest;
import com.medical.medcore.clinical.dto.OrderResultRequest;
import com.medical.medcore.clinical.dto.UpdatePatientClinicalRequest;
import com.medical.medcore.clinical.dto.MedicalEntryResponse;
import com.medical.medcore.clinical.dto.MedicalRecordResponse;
import com.medical.medcore.shared.security.annotation.RequireDoctorOrAdmin;
import com.medical.medcore.shared.security.annotation.RequireClinicalStaff;
import com.medical.medcore.clinical.service.MedicalRecordService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.shared.security.annotation.RequireModule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
@RequireModule(ClinicModule.CONSULTATION)
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @RequireClinicalStaff
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

    @RequireClinicalStaff
    @GetMapping("/entries/{entryId}")
    public ResponseEntity<ApiResponse<MedicalEntryResponse>> getEntry(@PathVariable Long entryId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.getEntry(entryId), "Entrada de historia clínica"));
    }

    @RequireClinicalStaff
    @GetMapping("/appointment/{appointmentId}/entries")
    public ResponseEntity<ApiResponse<List<MedicalEntryResponse>>> getByAppointment(
            @PathVariable Long appointmentId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.getEntriesByAppointment(appointmentId), "Atenciones de la cita"));
    }

    @RequireDoctorOrAdmin
    @PostMapping("/entries/{entryId}/sign")
    public ResponseEntity<ApiResponse<MedicalEntryResponse>> signEntry(@PathVariable Long entryId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.signEntry(entryId), "Nota firmada y bloqueada"));
    }

    @RequireDoctorOrAdmin
    @PostMapping("/orders/{orderId}/results")
    public ResponseEntity<ApiResponse<MedicalEntryResponse.OrderItem>> addOrderResult(
            @PathVariable Long orderId,
            @RequestBody OrderResultRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                medicalRecordService.addOrderResult(orderId, request), "Resultado registrado"));
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
