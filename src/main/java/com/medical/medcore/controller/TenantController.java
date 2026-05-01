package com.medical.medcore.controller;

import com.medical.medcore.dto.request.CreateTenantRequest;
import com.medical.medcore.dto.request.UpdateTenantRequest;
import com.medical.medcore.dto.response.TenantResponse;
import com.medical.medcore.security.authorization.annotation.RequireSuperAdmin;
import com.medical.medcore.service.tenant.TenantService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @RequireSuperAdmin
    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, tenantService.createTenant(request), "Tenant creado exitosamente"));
    }

    @RequireSuperAdmin
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, tenantService.updateTenant(id, request), "Tenant actualizado exitosamente"));
    }

    @RequireSuperAdmin
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, tenantService.getTenantById(id), "Tenant encontrado"));
    }

    @RequireSuperAdmin
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getAllTenants() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, tenantService.getAllTenants(), "Lista de tenants"));
    }

    @RequireSuperAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "Tenant eliminado exitosamente"));
    }
}
