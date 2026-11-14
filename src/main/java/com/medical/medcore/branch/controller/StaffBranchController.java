package com.medical.medcore.branch.controller;

import com.medical.medcore.branch.dto.BulkBranchesRequest;
import com.medical.medcore.branch.dto.StaffBranchResponse;
import com.medical.medcore.shared.security.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.branch.service.StaffBranchService;
import com.medical.medcore.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StaffBranchController {

    private final StaffBranchService staffBranchService;

    @RequireAdminOrSuperAdmin
    @GetMapping("/users/{userId}/branches")
    public ResponseEntity<ApiResponse<List<StaffBranchResponse>>> listBranchesForUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                staffBranchService.listBranchesForUser(userId), "Sucursales del usuario"));
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/users/{userId}/branches/{branchId}")
    public ResponseEntity<ApiResponse<StaffBranchResponse>> assign(
            @PathVariable Long userId, @PathVariable Long branchId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                staffBranchService.assign(userId, branchId), "Usuario asignado a la sucursal"));
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/users/{userId}/branches/bulk")
    public ResponseEntity<ApiResponse<List<StaffBranchResponse>>> bulkAssign(
            @PathVariable Long userId, @Valid @RequestBody BulkBranchesRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                staffBranchService.bulkAssign(userId, request.branchIds()), "Usuario asignado a las sucursales"));
    }

    @RequireAdminOrSuperAdmin
    @DeleteMapping("/users/{userId}/branches/{branchId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long userId, @PathVariable Long branchId) {
        staffBranchService.deactivate(userId, branchId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Usuario desvinculado de la sucursal"));
    }
}
