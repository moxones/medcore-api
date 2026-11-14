package com.medical.medcore.dashboard.controller;

import com.medical.medcore.dashboard.dto.DashboardSummaryResponse;
import com.medical.medcore.dashboard.dto.DoctorDashboardSummaryResponse;
import com.medical.medcore.dashboard.dto.DoctorProductivityResponse;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import com.medical.medcore.dashboard.service.DashboardService;
import com.medical.medcore.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@RequireStaff
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, dashboardService.getSummaryMetrics(), "Métricas del dashboard principal")
        );
    }

    @GetMapping("/productivity")
    public ResponseEntity<ApiResponse<List<DoctorProductivityResponse>>> getDoctorProductivity() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, dashboardService.getDoctorProductivity(), "Productividad mensual por médico")
        );
    }

    @GetMapping("/doctor-summary")
    public ResponseEntity<ApiResponse<DoctorDashboardSummaryResponse>> getDoctorSummary() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, dashboardService.getDoctorSummary(), "Resumen del dashboard del médico")
        );
    }
}
