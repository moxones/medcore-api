package com.medical.medcore.controller;

import com.medical.medcore.dto.request.BulkBranchesRequest;
import com.medical.medcore.dto.response.DoctorBranchResponse;
import com.medical.medcore.security.authorization.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.service.doctor.DoctorBranchService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DoctorBranchController {

    private final DoctorBranchService doctorBranchService;

    @GetMapping("/doctors/{doctorId}/branches")
    public ResponseEntity<ApiResponse<List<DoctorBranchResponse>>> listBranchesForDoctor(
            @PathVariable Long doctorId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorBranchService.listBranchesForDoctor(doctorId), "Sucursales del doctor"));
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/doctors/{doctorId}/branches/{branchId}")
    public ResponseEntity<ApiResponse<DoctorBranchResponse>> assign(
            @PathVariable Long doctorId, @PathVariable Long branchId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorBranchService.assign(doctorId, branchId), "Doctor asignado a la sucursal"));
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/doctors/{doctorId}/branches/bulk")
    public ResponseEntity<ApiResponse<List<DoctorBranchResponse>>> bulkAssign(
            @PathVariable Long doctorId, @Valid @RequestBody BulkBranchesRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorBranchService.bulkAssign(doctorId, request.branchIds()), "Doctor asignado a las sucursales"));
    }

    @RequireAdminOrSuperAdmin
    @DeleteMapping("/doctors/{doctorId}/branches/{branchId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long doctorId, @PathVariable Long branchId) {
        doctorBranchService.deactivate(doctorId, branchId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Doctor desvinculado de la sucursal"));
    }

    @GetMapping("/branches/{branchId}/doctors")
    public ResponseEntity<ApiResponse<List<DoctorBranchResponse>>> listDoctorsByBranch(
            @PathVariable Long branchId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorBranchService.listDoctorsByBranch(branchId), "Médicos de la sucursal"));
    }
}
