package com.medical.medcore.branch.controller;

import com.medical.medcore.branch.dto.BranchRequest;
import com.medical.medcore.branch.dto.BranchResponse;
import com.medical.medcore.branch.service.BranchService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.api.PageableResponse;
import com.medical.medcore.shared.security.annotation.RequireAdminOrSuperAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ApiResponse<PageableResponse<BranchResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ApiResponse.ok(branchService.findAll(page, size, includeInactive), "Sucursales");
    }

    @GetMapping("/{id}")
    public ApiResponse<BranchResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(branchService.findById(id), "Sucursal");
    }

    @RequireAdminOrSuperAdmin
    @PostMapping
    public ApiResponse<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
        return ApiResponse.ok(branchService.create(request), "Sucursal creada");
    }

    @RequireAdminOrSuperAdmin
    @PutMapping("/{id}")
    public ApiResponse<BranchResponse> update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return ApiResponse.ok(branchService.update(id, request), "Sucursal actualizada");
    }

    @RequireAdminOrSuperAdmin
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        branchService.deactivate(id);
        return ApiResponse.message("Sucursal desactivada");
    }
}
