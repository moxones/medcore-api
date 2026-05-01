package com.medical.medcore.controller;

import com.medical.medcore.dto.response.DashboardSummaryResponse;
import com.medical.medcore.dto.response.DoctorProductivityResponse;
import com.medical.medcore.service.dashboard.DashboardService;
import com.medical.medcore.types.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
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
}
