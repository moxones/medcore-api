package com.medical.medcore.controller;

import com.medical.medcore.dto.request.PatientAllergyRequest;
import com.medical.medcore.dto.request.PatientConditionRequest;
import com.medical.medcore.dto.request.PatientFamilyHistoryRequest;
import com.medical.medcore.dto.request.PatientHabitRequest;
import com.medical.medcore.dto.request.PatientSurgicalHistoryRequest;
import com.medical.medcore.dto.response.PatientClinicalHistoryResponse;
import com.medical.medcore.dto.response.PatientClinicalHistoryResponse.*;
import com.medical.medcore.security.authorization.annotation.RequireDoctorOrAdmin;
import com.medical.medcore.security.authorization.annotation.RequireStaff;
import com.medical.medcore.service.patienthistory.PatientHistoryService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/patients/{patientId}")
@RequiredArgsConstructor
public class PatientHistoryController {

    private final PatientHistoryService patientHistoryService;

    @RequireStaff
    @GetMapping("/clinical-history")
    public ResponseEntity<ApiResponse<PatientClinicalHistoryResponse>> getHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                patientHistoryService.getHistory(patientId), "Antecedentes del paciente"));
    }

    @RequireDoctorOrAdmin
    @PostMapping("/allergies")
    public ResponseEntity<ApiResponse<AllergyItem>> addAllergy(
            @PathVariable Long patientId, @Valid @RequestBody PatientAllergyRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                patientHistoryService.addAllergy(patientId, request), "Alergia registrada"));
    }

    @RequireDoctorOrAdmin
    @DeleteMapping("/allergies/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAllergy(@PathVariable Long patientId, @PathVariable Long id) {
        patientHistoryService.deleteAllergy(patientId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Alergia eliminada"));
    }

    @RequireDoctorOrAdmin
    @PostMapping("/conditions")
    public ResponseEntity<ApiResponse<ConditionItem>> addCondition(
            @PathVariable Long patientId, @Valid @RequestBody PatientConditionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                patientHistoryService.addCondition(patientId, request), "Condición registrada"));
    }

    @RequireDoctorOrAdmin
    @DeleteMapping("/conditions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCondition(@PathVariable Long patientId, @PathVariable Long id) {
        patientHistoryService.deleteCondition(patientId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Condición eliminada"));
    }

    @RequireDoctorOrAdmin
    @PostMapping("/family-history")
    public ResponseEntity<ApiResponse<FamilyHistoryItem>> addFamilyHistory(
            @PathVariable Long patientId, @Valid @RequestBody PatientFamilyHistoryRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                patientHistoryService.addFamilyHistory(patientId, request), "Antecedente familiar registrado"));
    }

    @RequireDoctorOrAdmin
    @DeleteMapping("/family-history/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFamilyHistory(@PathVariable Long patientId, @PathVariable Long id) {
        patientHistoryService.deleteFamilyHistory(patientId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Antecedente familiar eliminado"));
    }

    @RequireDoctorOrAdmin
    @PostMapping("/surgical-history")
    public ResponseEntity<ApiResponse<SurgicalHistoryItem>> addSurgicalHistory(
            @PathVariable Long patientId, @Valid @RequestBody PatientSurgicalHistoryRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                patientHistoryService.addSurgicalHistory(patientId, request), "Antecedente quirúrgico registrado"));
    }

    @RequireDoctorOrAdmin
    @DeleteMapping("/surgical-history/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSurgicalHistory(@PathVariable Long patientId, @PathVariable Long id) {
        patientHistoryService.deleteSurgicalHistory(patientId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Antecedente quirúrgico eliminado"));
    }

    @RequireDoctorOrAdmin
    @PostMapping("/habits")
    public ResponseEntity<ApiResponse<HabitItem>> addHabit(
            @PathVariable Long patientId, @Valid @RequestBody PatientHabitRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                patientHistoryService.addHabit(patientId, request), "Hábito registrado"));
    }

    @RequireDoctorOrAdmin
    @DeleteMapping("/habits/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHabit(@PathVariable Long patientId, @PathVariable Long id) {
        patientHistoryService.deleteHabit(patientId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Hábito eliminado"));
    }
}
