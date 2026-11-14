package com.medical.medcore.platform.controller;

import com.medical.medcore.platform.dto.CreateTenantRequest;
import com.medical.medcore.platform.dto.TenantResponse;
import com.medical.medcore.platform.dto.UpdateTenantRequest;
import com.medical.medcore.platform.service.TenantService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireSuperAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@RequireSuperAdmin
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ApiResponse<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        return ApiResponse.ok(tenantService.createTenant(request), "Organización creada");
    }

    @PutMapping("/{id}")
    public ApiResponse<TenantResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequest request) {
        return ApiResponse.ok(tenantService.updateTenant(id, request), "Organización actualizada");
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(tenantService.getTenantById(id), "Organización encontrada");
    }

    @GetMapping
    public ApiResponse<List<TenantResponse>> findAll() {
        return ApiResponse.ok(tenantService.getAllTenants(), "Lista de organizaciones");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        tenantService.deactivateTenant(id);
        return ApiResponse.message("Organización desactivada");
    }
}
