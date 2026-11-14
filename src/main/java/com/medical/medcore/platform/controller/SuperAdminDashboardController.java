package com.medical.medcore.platform.controller;

import com.medical.medcore.platform.dto.EnrichedTenantResponse;
import com.medical.medcore.platform.dto.GlobalDashboardSummaryResponse;
import com.medical.medcore.shared.security.annotation.RequireSuperAdmin;
import com.medical.medcore.platform.service.SuperAdminDashboardService;
import com.medical.medcore.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/super-admin/dashboard")
@RequiredArgsConstructor
public class SuperAdminDashboardController {

    private final SuperAdminDashboardService superAdminDashboardService;

    @RequireSuperAdmin
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<GlobalDashboardSummaryResponse>> getGlobalSummary() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, superAdminDashboardService.getGlobalSummary(), "Métricas globales del dashboard")
        );
    }

    @RequireSuperAdmin
    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<Page<EnrichedTenantResponse>>> getEnrichedTenants(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, superAdminDashboardService.getEnrichedTenants(pageable), "Lista de tenants enriquecida")
        );
    }
}
